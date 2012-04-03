(defproject caribou-api "0.1.0-SNAPSHOT"
  :description "The api ring handler for caribou-core"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [antler/caribou "0.3.4-SNAPSHOT"]
                 [compojure "0.6.4"]
                 ;; [ring/ring-core "1.0.2"]
                 [hiccup "0.3.6"]
                 [cheshire "2.0.2"]
                 [org.clojars.ninjudd/data.xml "0.0.1-SNAPSHOT"]
                 [clojure-csv/clojure-csv "1.3.2"]
                 [antler/sandbar "0.4.0-SNAPSHOT"]]
                 ;; [ring/ring-jetty-adapter "0.3.10"]
  :dev-dependencies [[lein-ring "0.4.5"]
                     [swank-clojure "1.4.0-SNAPSHOT"]]
  :jvm-opts ["-agentlib:jdwp=transport=dt_socket,server=y,suspend=n"]
  :ring {:handler caribou-api.core/app
         :servlet-name "caribou"
         :init caribou-api.core/init})