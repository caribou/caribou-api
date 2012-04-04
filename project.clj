(defproject caribou-api "0.1.0-SNAPSHOT"
  :description "The api ring handler for caribou-core"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [antler/caribou "0.4.0-SNAPSHOT"]
                 [compojure "1.0.1"]
                 [ring/ring-core "1.0.2"]
                 [hiccup "0.3.6"]
                 [org.clojars.doo/cheshire "2.2.3"]
                 ;; [cheshire "3.1.0"]
                 [org.clojure/data.xml "0.0.3"]
                 [clojure-csv/clojure-csv "1.3.2"]
                 [org.clojars.cjschroed/sandbar "0.4.0"]]
                 ;; [antler/sandbar "0.4.0-SNAPSHOT"]]
  :dev-dependencies [[lein-ring "0.6.2"]
                     [swank-clojure "1.4.0-SNAPSHOT"]]
  :jvm-opts ["-agentlib:jdwp=transport=dt_socket,server=y,suspend=n"]
  :ring {:handler caribou-api.core/app
         :servlet-name "caribou"
         :init caribou-api.core/init})