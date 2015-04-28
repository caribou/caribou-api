(ns caribou-api.test.core
  (:use [clojure.test])
  (:require [caribou.api.controllers.home :as ctrl]
            [caribou.api.util :as api-util]
            [caribou-api.test :as test]
            [caribou.migrations.bootstrap :as mig]
            [caribou.model :as model]
            [caribou.query :as query]
            [caribou.core :as core]
            [caribou.util :as util]
            [caribou.db :as db]
            [ring.mock.request :as mock]
            [ring.middleware.defaults :as ring-m]))

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
    (let [body (:body (ctrl/index {:params {:model "company.json"}}))]
      (is (.contains body "\"name\":\"Acme\""))
      (is (.contains body "\"name\":\"CorpInc\"")))))

(defn detail-test []
  (testing "Detail response"
    (let [body (:body (ctrl/detail
                       {:params {:model "company.json" :id "2"}}))]
      ;; id 2 is CorpInc
      ;; the ID for a detail request is not inside a :where, but
      ;; directly in the params.
      (is (not (.contains body "\"name\":\"Acme\"")))
      ;; should not contain any reference to Acme
      (is (.contains body "\"name\":\"CorpInc\"")))))

(defn delete-test []
  (testing "Delete response (single item)"
    (ctrl/delete {:params {:model "company.json" :where "id:2"}})
    (let [remaining-company (->> (model/gather :company)
                                 (map :name))]
      (is (not (some #{"CorpInc"} remaining-company))) ;; CorpInc should be gone
      (is (some #{"Acme"} remaining-company)))) ;; Acme should remain
  (testing "Delete response (everything)"
    (model/create :company {:name "CorpInc"}) ;; re-add the removed company
    (ctrl/delete {:params {:model "company.json"}}) ;; delete everything
    (let [remaining-company (->> (model/gather :company)
                                 (map :name))]
      (is (empty? remaining-company)))))
    
(defn create-test []
  (testing "Create response"
    ;; create a new item
    (-> (mock/request :post "" {:model "company"
                                :name "Megacorp"
                                :useless-field "boo!"})
        ((ring-m/wrap-defaults ctrl/create ring-m/api-defaults)))
    ;; now the new item should be in the DB
    (is (model/pick :company {:where {:name "Megacorp"}}))
    ;; make sure the superfluous field is not inserted into the DB
    (is (->> (model/pick :company {:where {:name "Megacorp"}})
             (keys)
             (some #{:useless-field})
             (not)))))



(defn update-test []
  (testing "Update response"
    ;; rename Acme as Megacorp
    (-> (mock/request :put "" {:model "company"
                               :id "1"
                               :name "Megacorp"
                               :useless-field "boo!"})
        ((ring-m/wrap-defaults ctrl/update ring-m/api-defaults)))
    (model/pick :company {:where {:id 1}})
    (is (= (:name (model/pick :company {:where {:id 1}}) "Megacorp")))))


(defn options-test []
  (testing "OPTIONS method"
    (let [response
          (-> (mock/request :options "" {:model "company"})
              ((ring-m/wrap-defaults ctrl/options ring-m/api-defaults)))]
      (is (= (get-in response [:headers "Allow"]) "GET, POST")))
    (let [response
          (-> (mock/request :options "" {:model "company" :id "1"})
              ((ring-m/wrap-defaults ctrl/options ring-m/api-defaults)))]
      (is (= (get-in response [:headers "Allow"]) "GET, PUT, DELETE")))))

        
;;; some sanitization / control of the allowed params

(defn overwrite-params []
  (testing "Overwrite params"
    (let [body (:body (ctrl/detail
                       {:params {:model "company.json" :id "2"}
                        :caribou.api.controllers.home/sanitize-fn
                        #(api-util/replace-all-values % {:id "1"})}))]
      ;; id 2 is CorpInc, but we should have overwritten the id to get Acme.
      (is (.contains body "\"name\":\"Acme\""))
      (is (not (.contains body "\"name\":\"CorpInc\""))))

    ;; overwrite params when creating an object
    (-> (mock/request :post "" {:model "company"
                                :name "Megacorp"})
        (assoc :caribou.api.controllers.home/sanitize-fn
          #(api-util/replace-all-values % {:name "Pokemon"}))
        ((ring-m/wrap-defaults ctrl/create ring-m/api-defaults)))
    ;; the new company should have been created under the name "Pokemon"
    (is (model/pick :company {:where {:name "Pokemon"}}))))


(defn all-model-tests
  []
  (db-fixture invoke-model-test)
  (db-fixture index-test)
  (db-fixture detail-test)
  (db-fixture delete-test)
  (db-fixture create-test)
  (db-fixture update-test)
  (db-fixture overwrite-params)
  (db-fixture options-test)
  )


;; We test in H2, because it doesn't rely on any external DB.
;; Other DBs, like postgresql, should already be tested in
;; caribou-core.

(deftest ^:h2
  h2-tests
  (let [config (test/read-config :h2)]
    (with-system-models config
      (all-model-tests))))
