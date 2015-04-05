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
        opts (select-keys (:params request) [:limit :offset :include :where :order])
        item (model/find-one slug opts)]
    (render format (wrap-response slug item))))

(defn create
  [request]
  (render
   :json
   {:ip :bar}))

(defn update
  [request]
  (render
   :json
   {:yellow :core}))

(defn delete
  [request]
  (let [model-slug (-> request :params :model keyword)
        [slug format] (split-format model-slug)
        opts (select-keys (:params request) [:limit :offset :include :where :order])
        items (model/find-all slug opts)]
    (doseq [item items :let [id (:id item)]]
      (model/destroy slug id))
    (render format (wrap-response slug items)))) ;; reference the deleted items
