(ns caribou-api.test.controllers
  (:use [clojure.test])
  (:require [caribou-api.test.base :as base]
            [caribou.api.controllers.home :as ctrl]
            [caribou.api.util :as api-util]
            [caribou.migrations.bootstrap :as mig]
            [caribou.model :as model]
            [caribou.query :as query]
            [caribou.core :as core]
            [caribou.util :as util]
            [caribou.db :as db]
            [ring.mock.request :as mock]
            [ring.middleware.defaults :as ring-m]))


(defn invoke-model-test
  []
  (let [model (util/query "select * from model where id = 1")
        invoked (model/invoke-model (first model))]
    (testing "Model invocation."
      (is (= "name" (-> invoked :fields :name :row :slug))))))

(defn index-test []
  (testing "Index"
    (let [body (:body (ctrl/index {:params {:model "company.json"}}))]
      (is (.contains body "\"name\":\"Acme\""))
      (is (.contains body "\"name\":\"CorpInc\"")))))

(defn detail-test []
  (testing "Detail"
    (let [body (:body (ctrl/detail
                       {:params {:model "company.json" :id "2"}}))]
      ;; id 2 is CorpInc
      ;; the ID for a detail request is not inside a :where, but
      ;; directly in the params.
      (is (not (.contains body "\"name\":\"Acme\"")))
      ;; should not contain any reference to Acme
      (is (.contains body "\"name\":\"CorpInc\"")))))

(defn delete-test []
  (testing "Delete with missing ID"
    (-> (ctrl/delete {:params {:model "company.json"}})
        (:status)
        (= 400)
        (is)))
  (testing "Delete (single item)"
    (ctrl/delete {:params {:model "company.json" :id 2}})
    (let [remaining-company (->> (model/gather :company)
                                 (map :name))]
      (is (not (some #{"CorpInc"} remaining-company))) ;; CorpInc should be gone
      (is (some #{"Acme"} remaining-company))))) ;; Acme should remain


(defn create-test []
  (testing "Create"
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
  (testing "Update"
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
  (base/db-fixture invoke-model-test)
  (base/db-fixture index-test)
  (base/db-fixture detail-test)
  (base/db-fixture delete-test)
  (base/db-fixture create-test)
  (base/db-fixture update-test)
  (base/db-fixture overwrite-params)
  (base/db-fixture options-test)
  )


;; We test in H2, because it doesn't rely on any external DB.
;; Other DBs, like postgresql, should already be tested in
;; caribou-core.

(deftest ^:h2
  h2-tests
  (let [config (base/read-config :h2)]
    (base/with-system-models config
      (all-model-tests))))
