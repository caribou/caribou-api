(ns caribou.api.util
  (:use [caribou.app.controller :only [render]])
  (:require [clojure.string :as string]
            [caribou.model :as model]
            [caribou.logger :as log]
            [clojure.string :as s]))



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
      (update-in [:where] model/process-where)))

(defn split-format
  [model]
  (let [[key format] (string/split (name model) #"\.")]
    [(keyword key) (or format :json)]))
