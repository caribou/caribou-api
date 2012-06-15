(ns caribou.api.halo
  (:require [caribou.config :as config]
            [clj-http.client :as http-client]))

;; ===============================
;; Halo Connectors
;; ===============================

(defn halo-endpoint
  "Define a halo endpoint for the given host and route."
  [host route-str]
  (str host (@config/app :halo-prefix) "/" route-str))

(defn halo-endpoints
  "Get all endpoints given by the :halo-hosts configuration."
  [route-str]
  (map #(halo-endpoint % route-str) (@config/app :halo-hosts)))

(defn halo-headers
  "Returns a map of halo required headers."
  []
  {"X-Halo-Key" (@config/app :halo-key)})

(defn reload-routes
  "Trigger the reloading of all page routes."
  [env]
  (let [endpoints (halo-endpoints "reload-routes")]
    (doseq [endpoint endpoints]
      (http-client/get endpoint {:headers (halo-headers)})))
  env)

(defn reload-models
  "Trigger the reloading of every model."
  [env]
  (let [endpoints (halo-endpoints "reload-models")]
    (doseq [endpoint endpoints]
      (http-client/get endpoint {:headers (halo-headers)})))
  env)
