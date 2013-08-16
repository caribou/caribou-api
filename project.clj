(defproject antler/caribou-api "0.11.24"
  :description "The api ring handler for caribou"
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [antler/caribou-frontend "0.11.30"]
                 [clj-http "0.5.6"
                  :exclusions [org.apache.httpcomponents/httpclient
                               org.apache.httpcomponents/httpcore]]]
  :main caribou.api.core
  :jvm-opts ["-agentlib:jdwp=transport=dt_socket,server=y,suspend=n"]
  :autodoc {:name "Caribou API"
            :page-title "Caribou API - Documentation"
            :description
            "<p>This HTTP API is automatically generated if a database managed by
             <a href=\"http://antler.github.com/caribou-core\">Caribou Core</a>
             is present."}
  :ring {:handler caribou.api.core/handler
         :servlet-name "caribou-api"
         :init caribou.api.core/init
         :port 33443})
