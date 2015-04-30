(ns caribou.api.middleware
  (:require [caribou.api.util :refer [split-format]]
            [caribou.app.controller :refer [render]]))

;;; Some middlewares to help control how the data is made available
;;; thought the API.


(defn wrap-sanitizing-fn
  "Insert a sanitizing function into the request. This function is the
  last chance to intercept the map built from the request parameters
  before it is used on the database. 

  The provided function should accept the request and the parameters
  map. 
  ---> (sani-fn request {:where... :include...})"
  [handler sanitize-fn]
  (fn [request]
    (handler (assoc request :caribou.api.controllers.home/sanitize-fn
                    (partial sanitize-fn request)))))


(defn wrap-allowed-models
  "Prevent some models from being accessible from the API.
  `model-allowed?' is a function accepting the model keyword. If the
  function returns false, the handler isn't evaluated and return a
  http 403 error."
  [handler model-allowed?]
  (fn [request]
    (let [[request-model format] (-> request :params :model split-format)]
      (if (model-allowed? request-model)
        (handler request)
        (render format
         {:status 403
          :headers {}
          :response "403 Forbidden - This model cannot be accessed by the API"})))))



;;;;;;;;;;;;;;;;;


;; maybe use some more fine grained methods like :create and :update?
(defn- operation-type
  "Given a request-method, return an operation type :read or :write"
  [request-method]
  (if (some #{request-method} [:put :delete :post])
    :write
    :read))

(defn- operation-allowed? [request allowed-operation-map]
  (let [request-model (-> request :params :model split-format first)
        permissions (get allowed-operation-map request-model)]
    (when (some #{(operation-type (:request-method request))} permissions)
      true)))


;; this should probably return a 405
(defn wrap-allowed-operation
  "Control the allowed operations on models (read/write).

  'model-allowed-op-fn' should accept the model (slug) and return a
  collection of permissions [:read :write]."
  [handler model-allowed-op-fn]
  (fn [request]
    (if-let [model (-> request :params :model)]
      (let [[request-model format] (split-format model)
            op-type (-> request :request-method operation-type)]
        (if (some #{op-type} (model-allowed-op-fn request-model))
          (handler request)
          (render format
                  {:status 403
                   :headers {}
                   :response "403 Forbidden - Insufficient rights"})))
      (handler request))))




