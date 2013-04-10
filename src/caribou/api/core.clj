(ns caribou.api.core
  (:use [ring.middleware.json-params :only (wrap-json-params)]
        [ring.middleware.multipart-params :only (wrap-multipart-params)]
        [ring.middleware.params :only (wrap-params)]
        [ring.middleware.file :only (wrap-file)]
        [ring.middleware.head :only (wrap-head)]
        [ring.middleware.file-info :only (wrap-file-info)]
        [ring.middleware.resource :only (wrap-resource)]
        [ring.middleware.nested-params :only (wrap-nested-params)]
        [ring.middleware.keyword-params :only (wrap-keyword-params)]
        [ring.middleware.reload :only (wrap-reload)]
        [ring.middleware.session :only (wrap-session)]
        [ring.middleware.session.cookie :only (cookie-store)]
        [ring.middleware.cookies :only (wrap-cookies)]
        [ring.middleware.content-type :only (wrap-content-type)]
        [cheshire.core :only (generate-string encode)]
        [cheshire.custom :only (add-encoder)]
        [ring.util.response :only (redirect)])
  (:require [clojure.string :as string]
            [clojure.java.jdbc :as sql]
            [clojure.java.io :as io]
            [clojure-csv.core :as csv]
            [clojure.data.xml :as xml]
            [swank.swank :as swank]
            [lichen.core :as lichen]
            [caribou.config :as config]
            [caribou.db :as db]
            [caribou.model :as model]
            [caribou.field :as field]
            [caribou.association :as association]
            [caribou.util :as util]
            [caribou.logger :as log]
            [caribou.asset :as asset]
            [caribou.app.i18n :as i18n]
            [caribou.app.pages :as pages]
            [caribou.app.template :as template]
            [caribou.app.halo :as halo]
            [caribou.app.middleware :as middleware]
            [caribou.app.request :as request]
            [caribou.api.routes :as routes]
            [caribou.logger :as log]
            [caribou.app.handler :as handler]))

(declare handler)

(defn reload-pages
  []
  (pages/add-page-routes routes/api-routes 'caribou.api.controllers "/_api"))

(defn api-wrapper
  [handler]
  (fn [request]
    (handler request)))

(defn init
  []
  (config/init)
  (model/init)
  (i18n/init)
  (template/init)
  (reload-pages)
  (halo/init
   {:reload-pages reload-pages
    :halo-reset handler/reset-handler})
  (def handler
    (-> (handler/handler)
        (api-wrapper)
        (wrap-reload)
        (wrap-file (@config/app :asset-dir))
        (wrap-resource (@config/app :public-dir))
        (wrap-file-info)
        (wrap-head)
        (lichen/wrap-lichen (@config/app :asset-dir))
        (middleware/wrap-servlet-path-info)
        (middleware/wrap-xhr-request)
        (request/wrap-request-map)
        (wrap-json-params)
        (wrap-multipart-params)
        (wrap-keyword-params)
        (wrap-nested-params)
        (wrap-params)
        (db/wrap-db @config/db)
        ;; (wrap-session {:store (cookie-store {:key "vEanzxBCC9xkQUoQ"})
        ;;                :cookie-name "caribou-admin-session"
        ;;                :cookie-attrs {:max-age (days-in-seconds 90)}})
        (wrap-cookies)))

  (swank/start-server :host "127.0.0.1" :port 4007))
