(ns caribou-api.test.base
  (:use [clojure.test])
  (:require [clojure.java.io :as io]
            [caribou.config :as config]
            [caribou.api.controllers.home :as ctrl]
            [caribou.api.util :as api-util]
            [caribou.migrations.bootstrap :as mig]
            [caribou.model :as model]
            [caribou.query :as query]
            [caribou.core :as core]
            [caribou.util :as util]
            [caribou.db :as db]
            [ring.mock.request :as mock]
            [ring.middleware.defaults :as ring-m]
            ))


(defn read-config
  [db]
  (-> (str "config/test-" (name db) ".clj")
      io/resource
      config/read-config
      (merge  {:logging {:loggers [{:type :stdout :level :warning}]}})
      ;; no need to print everything...
      core/init))


(defmacro with-system-models
  "Create the system models, evaluate the body, remove the system
  models and return the result obtained with the body.

  In addition, the body is already wrapped into a with-caribou."
  [config & body]
  `(core/with-caribou ~config ;; run everything with the config environment
     (try (mig/rollback) ;; clear the db, just to be sure
          (catch Exception _#))
     (mig/migrate) ;; initialize the system models (model, field, domain, asset...)    
     (let [result# (do ~@body)]
       (mig/rollback) ;; clean the db
       result#))) ;; return the result of 'body'


(defn test-init
  []
  (model/invoke-models)
  (query/clear-queries))

(defn db-fixture
  [f]
  (test-init)
  ;; create some initial companies
  (model/create :model {:name "Company"
                        :fields [{:name "Name" :type "string"}]})
  (model/create :company {:name "Acme"}) ;; first item created ---> ID = 1
  (model/create :company {:name "CorpInc"}) ;; second item created ---> ID = 2
  (f)
  ;; delete the companies
  (doseq [slug [:company]]
    (when (db/table? slug) (model/destroy :model (model/models slug :id)))))


;;; All tests should be created to work into the `db-fixture`.
;;; This allows us to make sure we don't leave traces in the DB.
