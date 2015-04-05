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

(defmacro with-system-models [config & body]
  `(do
     (core/with-caribou ~config ;; run everything with the config environment
       (try (mig/rollback) ;; clear the db, just to be sure
            (catch Exception _#))
       (mig/migrate) ;; initialize the system models (model, field, domain, asset...)    
       ~@body
       (mig/rollback)))) ;; clean the db


(defn invoke-model-test
  []
  (let [model (util/query "select * from model where id = 1")
        invoked (model/invoke-model (first model))]
    (testing "Model invocation."
      (is (= "name" (-> invoked :fields :name :row :slug))))))


(defn test-init
  []
  (model/invoke-models)
  (query/clear-queries))

(defn db-fixture
  [f]
  (test-init)
  (f)
  (doseq [slug [:yellow :purple :zap :chartreuse :fuchsia :base :level
                :void :white :agent :pinkƒpink1]]
    (when (db/table? slug) (model/destroy :model (model/models slug :id)))
    (when (db/table? :everywhere)
      (do
        (db/do-sql "delete from locale")
        (model/destroy :model (model/models :nowhere :id))
        (model/destroy :model (model/models :everywhere :id))))
    (when-let [passport (model/pick :asset {:where
                                            {:filename "passport.picture"}})]
      (model/destroy :asset (:id passport)))))


(defn all-model-tests
  []
  (db-fixture invoke-model-test))


(deftest ^:h2
  h2-tests
  (let [config (test/read-config :h2)]
    (with-system-models config
      (all-model-tests)
      )))



;; (deftest content-list-test
;;   (sql/with-connection (t/read-config :h2)
;;     (model/invoke-models)
;;     (is (> (count (ctrl/index "model" {})) 0))))

;; (deftest content-list-test
;;   (sql/with-connection (t/read-config :h2)
;;     (model/invoke-models)
;;     (is (> (count (content-list "model" {})) 0))))

;; (deftest content-item-test
;;   (sql/with-connection (t/read-config :h2)
;;     (model/invoke-models)
;;     (is (> (count (content-item "model" 1)) 0))))

;; (deftest content-field-test
;;   (sql/with-connection (t/read-config :h2)
;;     (model/invoke-models)
;;     (is (= "model" (content-field "model" 1 :slug)))))

;; ;; TODO: test timestamp fields
;; ;; (deftest render-test
;; ;;   (sql/with-connection (t/read-config :h2)
;; ;;     (let [model (render :model (db/fetch) {})]
;; ;;       (is (not (model nil)))
;; ;;       (is (= (model :name) "Foo"))
;; ;;       (is (= (model :description) "bar"))
;; ;;       (is (= (model :position) 1))
;; ;;       (is (= (model :nested) false))
;; ;;       (is (= (model :abstract) false))
;; ;;       (is (= (model :ancestor_id) 0)))))

;; (deftest render-field-test
;;   (sql/with-connection (t/read-config :h2)
;;     (model/invoke-models)
;;     (is (= "yayay" (render-field "model" {:description "yayay"} "description" {})))))

;; ;; actions ------------------------------------------------
;; ;; happy-path action smoke-testing

;; ;; GET home
;; (deftest home-action-test
;;   (sql/with-connection (t/read-config :h2)
;;     (model/invoke-models)
;;     (let [response (json/read-json (home {}))]
;;       (is (not (response nil))))))

;; ;; GET list-all
;; (deftest list-all-action-test
;;   (sql/with-connection (t/read-config :h2)
;;     (model/invoke-models)
;;     (let [response (json/read-json (list-all {:slug "model"}))]
;;       (is (> (count response) 0)))))

;; ;; TODO
;; ;; POST create-content
;; (deftest create-content-action-test)

;; ;; GET model-spec
;; (deftest model-spec-action-test
;;   (sql/with-connection (t/read-config :h2)
;;     (model/invoke-models)
;;     (let [response (json/read-json (model-spec {:slug "model"}))]
;;       (is (> (count response) 0)))))

;; ;; GET item-detail
;; (deftest item-detail-action-test
;;   (sql/with-connection (t/read-config :h2)
;;     (model/invoke-models)
;;     (let [response (json/read-json (item-detail {:slug "model" :id 1}))]
;;       (is (> (count response) 0)))))

;; ;; TODO
;; ;; PUT update-content
;; (deftest update-content-action-test)

;; ;; TODO
;; ;; PUT delete-content
;; (deftest delete-content-action-test)

;; ;; GET field-detail
;; ;; (deftest field-detail-action-test
;; ;;   (sql/with-connection (t/read-config :h2)
;; ;;     (let [response (json/read-json (field-detail {:slug "model" :id 1 :field "name"}))]
;; ;;       (is (= "model" response)))))
