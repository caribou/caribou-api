(defproject antler/caribou-api "0.5.0"
  :description "The api ring handler for caribou"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [antler/caribou-core "0.6.3"]
                 [compojure "1.1.0-RC1"]
                 ;; [ring/ring-core "1.1.0"]
                 [ring "1.1.0"
                  :exclusions [org.clojure/clojure
                               clj-stacktrace
                               hiccup]]
                 [org.clojars.doo/cheshire "2.2.3"]
                 [org.clojure/data.xml "0.0.3"]
                 [clojure-csv/clojure-csv "1.3.2"]
                 ;; [antler/sandbar "0.4.0"]
                 [clj-http "0.3.6"]]
  :main caribou.api.core
  :jvm-opts ["-agentlib:jdwp=transport=dt_socket,server=y,suspend=n"]
  :ring {:handler caribou.api.core/app
         :servlet-name "caribou-api"
         :init caribou.api.core/init
         :configurator
         ~(fn [jetty]
            (doseq [connector (.getConnectors jetty)]
              (.setRequestHeaderSize connector 8388608)))
         :port 33443})
  ;; :repositories {"snapshots" {:url "http://battlecat:8080/nexus/content/repositories/snapshots" 
  ;;                             :username "deployment" :password "deployment"}
  ;;                "releases"  {:url "http://battlecat:8080/nexus/content/repositories/releases" 
  ;;                             :username "deployment" :password "deployment"}})

