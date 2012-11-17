(defproject antler/caribou-api "0.7.14"
  :description "The api ring handler for caribou"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [antler/caribou-core "0.7.13"]
                 [compojure "1.1.3" :exclusions [ring/ring-core]]
                 [ring "1.1.6"]
                 [cheshire "4.0.3"]
                 [org.clojure/data.xml "0.0.6"]
                 [clojure-csv "1.3.2" :exclusions [org.clojure/clojure]]
                 [swank-clojure "1.4.2"]
                 ;; [antler/sandbar "0.4.0"]
                 [clj-http "0.5.5"]]
  :main caribou.api.core
  :jvm-opts ["-agentlib:jdwp=transport=dt_socket,server=y,suspend=n"]
  :autodoc {:name "Caribou API"
            :page-title "Caribou API - Documentation"
            :description
            "<p>This HTTP API is automatically generated if a database managed by
             <a href=\"http://antler.github.com/caribou-core\">Caribou Core</a>
             is present."}
  :ring {:handler caribou.api.core/app
         :servlet-name "caribou-api"
         :init caribou.api.core/init
         :configurator
         ~(fn [jetty]
            (doseq [connector (.getConnectors jetty)]
              (.setRequestHeaderSize connector 8388608)))
         :port 33443})
