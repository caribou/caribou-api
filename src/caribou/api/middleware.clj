(ns caribou.api.middleware
  (:require [caribou.api.util :refer [split-format]]))

;;; Some minimal control over which models are accessible


(defn wrap-forbidden-models
  "Prevent some models from being accessible from the API.
  `model-allowed?' is a function accepting the model keyword. If the
  function returns false, the handler isn't evaluated and return a
  http 403 error."
  [handler model-allowed?]
  (fn [request]
    (let [request-model (-> request :params :model split-format first)]
      (if (model-allowed? request-model)
        (handler request)
        {:status 403
         :headers {}
         :body "403 Forbidden - This model cannot be accessed by the API"}))))

;;;;;;;;;;;;;;;;;


(defn operation-type [request-method]
  "Given a request-method, return an operation type :read or :write"
  (if (some #{request-method} [:put :delete :post])
    :write
    :read))

(defn operation-allowed? [request allowed-operation-map]
  (let [request-model (-> request :params :model split-format first)
        permissions (get allowed-operation-map request-model)]
    (when (some #{(operation-type (:request-method request))} permissions)
      true)))



;; this should probably return a 405, but giving a list of allowed
;; methods would assume other functions won't further restrict
;; anything.
(defn wrap-allowed-operation
  "Control the allowed operations on models (read/write) by a map of
  models and their associated operation.

  {:model-name [:read :write]
   :other-model [:read]}"
  [handler allowed-operation-map]
  (fn [request]
    (if (operation-allowed? request)
      (handler request)
      {:status 403
       :headers {}
       :body "403 Forbidden - Insufficient rights"})))


;;;;;;;;;;;;;;;;;;;

