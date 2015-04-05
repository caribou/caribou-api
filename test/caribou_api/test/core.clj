(ns caribou-api.test.core
  (:use [clojure.test])
  (:require [caribou-api.test :as test]
            [caribou.migrations.bootstrap :as mig]
            [caribou.model :as model]
            [caribou.query :as query]
            [caribou.core :as core]
            [caribou.config :as cc]
            [caribou.util :as util]
            [caribou.db :as db]))

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



(defn invoke-model-test
  []
  (let [model (util/query "select * from model where id = 1")
        invoked (model/invoke-model (first model))]
    (testing "Model invocation."
      (is (= "name" (-> invoked :fields :name :row :slug))))))

(defn index-test []
  (testing "Index response"
    (let [body (:body (caribou.api.controllers.home/index {:params {:model "company.json"}}))]
      (is (.contains body "\"name\":\"Acme\""))
      (is (.contains body "\"name\":\"CorpInc\"")))))

(defn detail-test []
  (testing "Detail response"
    (let [body (:body (caribou.api.controllers.home/detail
                       {:params {:model "company.json" :where "id:2"}}))] ;; id 2 is CorpInc
      (is (not (.contains body "\"name\":\"Acme\""))) ;; should not contain any reference to Acme
      (is (.contains body "\"name\":\"CorpInc\"")))))




(defn all-model-tests
  []
  (db-fixture invoke-model-test)
  (db-fixture index-test)
  (db-fixture detail-test))


;; We test in H2, because it doesn't rely on any external DB.
;; Other DBs, like postgresql, should already be tested in
;; caribou-core.

(deftest ^:h2
  h2-tests
  (let [config (test/read-config :h2)]
    (with-system-models config
      (all-model-tests))))
