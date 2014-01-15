(defproject caribou/caribou-api "0.13.8"
  :description "The api ring handler for caribou"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [caribou/caribou-frontend "0.13.8"]
                 [clj-http "0.5.6"
                  :exclusions [org.apache.httpcomponents/httpclient
                               org.apache.httpcomponents/httpcore]]]
  :jvm-opts ["-agentlib:jdwp=transport=dt_socket,server=y,suspend=n"]
  :ring {:handler caribou.api.core/handler
         :servlet-name "caribou-api"
         :init caribou.api.core/init
         :port 33443})
