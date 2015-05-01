(ns caribou.api.util
  (:use [caribou.app.controller :only [render]])
  (:require [clojure.string :as string]
            [caribou.model :as model]
            [caribou.logger :as log]
            [clojure.string :as s]))


;;; This section should be moved into caribou.model namespace to be
;;; with the other 'process' functions.

;;; Furthermore, it seems the other process functions can't handle
;;; maps with multiple nested values. For example:
;;; (model/process-where "drone,main-pilot.company:name:id,main-pilot:name")
;;; --> {:main-pilot "name", :drone nil}
;;; Some values are clearly discarded.

;;; process-fields keeps everything:
;;; (process-fields "drone,main-pilot.company:name:id,main-pilot:name")
;;; ([:drone] {:main-pilot [{:company (:name :id)} (:name)]})

(defn- recursive-merge-fields
  [a b]
   (if (and (map? a) (map? b))
     (merge-with recursive-merge-fields a b)
     [a b]))

(defn- ensure-vector
  "Nest all the maps into a vector."
  [m]
  (clojure.walk/postwalk
   (fn [x]
     (if-not (map? x) x
             (into {}
                   (for [[k v] x]
                     (if (map? v)
                       [k [v]]
                       [k v]))))) m))

(defn- build-path [path fields]
  (->> (clojure.string/split path #"\.")         
       (map keyword)
       ((fn[ks] (assoc-in {} ks (map keyword fields))))))

(defn process-fields [s]
  (when (seq s)
    (let [fields (string/split s #",")
          pre-processed (for [f fields]
                          (let [[path & fields] (seq (string/split f #":"))]
                            (if (seq fields)
                              (build-path path fields)
                              [(keyword path)])))
          grouped (group-by map? pre-processed)
          top-level-fields (get grouped false)
          merged-nested (when-let [nested (get grouped true)]
                          (-> (reduce recursive-merge-fields nested)
                              (ensure-vector)))]
      (println top-level-fields)
      (println merged-nested)
      (-> (conj top-level-fields merged-nested)
          ((partial remove nil?))))))

;"drone,main-pilot.company:name:id,main-pilot:name"
; "?include=fields.link&fields=fields:name:link.slug"  


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;






;;; some helpers to control queries

(defn- replace-value
  "Will replace all the values of a given key in nested maps."
  [m k v]
  (clojure.walk/prewalk
   (fn [x]
     (if (get x k) (assoc x k v) x)) m))


(defn replace-all-values
  "Will replace all the values of key in a nested maps with those of
  the replacement map."
  [original-map replacement-map]
  (reduce (fn [m [k v]]
            (replace-value m k v))
          original-map
          replacement-map))

;;;;;;;;;;;;



(defn queries-to-map
  "Convert the raw string query {:where \"name:bob\"} into a map:
   {:where {:name \"bob\"}}"
  [opts]
  (-> opts
      (update-in [:include] model/process-include)
      (update-in [:order] model/process-order)
      (update-in [:where] model/process-where)
      (update-in [:fields] process-fields)))

(defn split-format
  [model]
  (let [[key format] (string/split (name model) #"\.")]
    [(keyword key) (or format :json)]))
