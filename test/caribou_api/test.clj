(ns caribou-api.test
  (:require [clojure.java.io :as io]
            [caribou.config :as config]
            [caribou.core :as core]))

(defn read-config
  [db]
  (-> (str "config/test-" (name db) ".clj")
      io/resource
      config/read-config
      (merge  {:logging {:loggers [{:type :stdout :level :warning}]}}) ;; no need to print everything...
      core/init))
