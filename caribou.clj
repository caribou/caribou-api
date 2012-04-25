(use '[caribou.config :only (configure)])

(def default-config
  {
    :debug        true
    :use-database true
    :halo-enabled true
    :halo-prefix "/_halo"
    :halo-key    "9i209idfs09ugf0d9ug0fdsu09fdgis90dfgiigf0d-sgj0d9fgim,f09dgk"
    :database {
        :classname    "org.postgresql.Driver"
        :subprotocol  "postgresql"
        :host         "10.0.1.241"
        :database     "thejourney"
        :user         "nikebetterworld"
        :password     "worldbetternike"
    }
    :template-dir   "resources/templates" 
    :controller-ns  "thejourney.controllers"
    :public-dir     "resources/public"
  })

(defn get-config
  []
  default-config)

;; This call is required by Caribou
(configure (get-config))