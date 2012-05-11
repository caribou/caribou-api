(defproject antler/caribou-api "0.3.8"
  :description "The api ring handler for caribou"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [antler/caribou-core "0.5.3"]
                 [compojure "1.0.4"]
                 [ring/ring-core "1.1.0"
                  :exclusions [org.clojure/clojure
                               clj-stacktrace]]
                 [org.clojars.doo/cheshire "2.2.3"]
                 [org.clojure/data.xml "0.0.3"]
                 [clojure-csv/clojure-csv "1.3.2"]
                 [org.clojars.cjschroed/sandbar "0.4.0"]
                 [clj-http "0.3.6"]]
  :main caribou.api.core
  :jvm-opts ["-agentlib:jdwp=transport=dt_socket,server=y,suspend=n"]
  :ring {:handler caribou.api.core/app
         :servlet-name "caribou-api"
         :init caribou.api.core/init
         :port 33443}
  :repositories {"snapshots" {:url "http://battlecat:8080/nexus/content/repositories/snapshots" 
                              :username "deployment" :password "deployment"}
                 "releases"  {:url "http://battlecat:8080/nexus/content/repositories/releases" 
                              :username "deployment" :password "deployment"}})

