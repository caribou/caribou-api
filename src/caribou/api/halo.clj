(ns caribou.api.halo
  (:require [caribou.config :as config]
            [clj-http.client :as http-client]))

;; ===============================
;; Halo Connectors
;; ===============================

(defn halo-endpoint
  [host route-str]
  (str host (@config/app :halo-prefix) "/" route-str))

(defn halo-endpoints
  [route-str]
  (map #(halo-endpoint % route-str) (@config/app :halo-hosts)))

(defn halo-headers
  []
  {"X-Halo-Key" (@config/app :halo-key)})

(defn reload-routes
  [env]
  (let [endpoints (halo-endpoints "reload-routes")]
    (doseq [endpoint endpoints]
      (http-client/get endpoint {:headers (halo-headers)})))
  env)

(defn reload-models
  [env]
  (let [endpoints (halo-endpoints "reload-models")]
    (doseq [endpoint endpoints]
      (http-client/get endpoint {:headers (halo-headers)})))
  env)
