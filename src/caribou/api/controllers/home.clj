(ns caribou.api.controllers.home
  (:use [caribou.app.controller :only [render]])
  (:require [clojure.string :as string]
            [caribou.model :as model]
            [caribou.logger :as log]))

(defn wrap-response
  [slug response]
  {:meta {:status 200 :msg "OK" :type (name slug)}
   :response response})

(defn split-format
  [model]
  (let [[key format] (string/split (name model) #"\.")]
    [(keyword key) (or format :json)]))

(defn home
  [request]
  (render
   :json
   {:whats :up}))

(defn index
  [request]
  (let [model-slug (-> request :params :model keyword)
        [slug format] (split-format model-slug)
        opts (select-keys (:params request) [:limit :offset :include :where :order])
        items (model/find-all slug opts)]
    (render format (wrap-response slug items))))


(defn detail
  [request]
  (let [model-slug (-> request :params :model keyword)
        [slug format] (split-format model-slug)
        item (model/pick slug {:where {:id (-> request :params :id Integer/parseInt)}})]
    (render format (wrap-response slug item))))


(defn create ;; should we throw an error if we provide an ID?
             ;; (model/create with an ID is an update)
  [request]
  (let [model-slug (-> request :params :model keyword)
        [slug format] (split-format model-slug)
        spec (:params request)
        new-item (model/create slug spec)]
    (render format
            {:meta {:status 201 :msg "Created" :type (name slug)
                    :response new-item}})))


(defn update
  [request]
  (let [model-slug (-> request :params :model keyword)
        [slug format] (split-format model-slug)
        spec (:params request)]
    (if-not (:id spec)
      (render format {:meta {:status 400 :msg "Missing ID" :type (name slug)}
                      :response ""})
      (let [updated-item (model/create slug spec)]
        (render format (wrap-response slug updated-item))))))


(defn delete
  [request]
  (let [model-slug (-> request :params :model keyword)
        [slug format] (split-format model-slug)
        opts (select-keys (:params request) [:limit :offset :include :where :order])
        items (model/find-all slug opts)]
    (doseq [item items :let [id (:id item)]]
      (model/destroy slug id))
    (render format
            {:meta {:status 204 :msg "Deleted" :type (name slug)}
             :response ""})))
