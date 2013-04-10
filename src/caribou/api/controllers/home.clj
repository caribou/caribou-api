(ns caribou.api.controllers.home
  (:use [caribou.app.controller :only [render]])
  (:require [caribou.model :as model]
            [caribou.logger :as log]))

(defn wrap-response
  [response]
  {:meta {:status 200 :msg "OK"}
   :response response})

(defn home
  [request]
  (render
   :json
   {:whats :up}))

(defn index
  [request]
  (let [model-slug (-> request :params :model keyword)
        opts (select-keys (:params request) [:limit :offset :include :where :order])
        items (model/find-all model-slug opts)]
    (render :json (wrap-response items))))

(defn detail
  [request]
  (let [model-slug (-> request :params :model keyword)
        opts (select-keys (:params request) [:limit :offset :include :where :order])
        items (model/find-one model-slug opts)]
    (render :json (wrap-response items))))

(defn create
  [request]
  (render
   :json
   {:ip :bar}))

(defn update
  [request]
  (render
   :json
   {:yellow :crude}))

(defn delete
  [request]
  (render
   :json
   {:pink :gravity}))