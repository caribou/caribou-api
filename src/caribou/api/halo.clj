(ns caribou.api.halo
  (:require 
          [caribou.config :as config]
          [clj-http.client :as http-client]))
  
;; ===============================
;; Halo Connectors
;; ===============================

(defn halo-endpoint
  [route-str]
  (str (@config/app :halo-host) (@config/app :halo-prefix) "/" route-str))

(defn halo-headers
  []
  {"X-Halo-Key" (@config/app :halo-key)})

(defn reload-routes
  [env]
  (let [halo-endpoint (halo-endpoint "reload-routes")]
    (http-client/get halo-endpoint {:headers (halo-headers)}))
  env)

(defn reload-models
  [env]
  (let [halo-endpoint (halo-endpoint "reload-models")]
    (http-client/get halo-endpoint {:headers (halo-headers)}))
  env)
