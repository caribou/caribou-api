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
        [ring.middleware.content-type :only (wrap-content-type)])
  (:require [lichen.core :as lichen]
            [caribou.config :as config]
            [caribou.db :as db]
            [caribou.model :as model]
            [caribou.field :as field]
            [caribou.association :as association]
            [caribou.util :as util]
            [caribou.logger :as log]
            [caribou.asset :as asset]
            [caribou.core :as caribou]
            [caribou.app.pages :as pages]
            [caribou.app.template :as template]
            [caribou.app.middleware :as middleware]
            [caribou.app.request :as request]
            [caribou.app.config :as app-config]
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

(def config (app-config/default-config))

(defn init
  []
  (let [config (caribou/init config)]
    (caribou/with-caribou config
      (reload-pages)
      (def handler
        (-> (handler/handler #'reload-pages)
            (api-wrapper)
            (wrap-reload)
            (wrap-file (config/draw :assets :dir))
            (wrap-resource (config/draw :app :public-dir))
            (wrap-file-info)
            (wrap-head)
            (lichen/wrap-lichen (config/draw :assets :dir))
            (middleware/wrap-servlet-path-info)
            (middleware/wrap-xhr-request)
            (request/wrap-request-map)
            (wrap-json-params)
            (wrap-multipart-params)
            (wrap-keyword-params)
            (wrap-nested-params)
            (wrap-params)
            (handler/wrap-caribou config)
            ;; (wrap-session {:store (cookie-store {:key "vEanzxBCC9xkQUoQ"})
            ;;                :cookie-name "caribou-admin-session"
            ;;                :cookie-attrs {:max-age (days-in-seconds 90)}})
            (wrap-cookies))))))
