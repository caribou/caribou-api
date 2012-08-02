(ns caribou.api.core
  (:use compojure.core
        [cheshire.core :only (generate-string encode)]
        [cheshire.custom :only (add-encoder)]
        [ring.util.response :only (redirect)])
  (:use caribou.debug)
  (:require [clojure.string :as string]
            [swank.swank :as swank]
            [caribou.db :as db]
            [caribou.model :as model]
            [caribou.util :as util]
            [caribou.config :as config]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [clojure.java.jdbc :as sql]
            [clojure.java.io :as io]
            [clojure-csv.core :as csv]
            [clojure.data.xml :as xml]
            [caribou.api.halo :as api-halo]))

(def error
  {:meta {:status "500"
          :msg "Unable to process request"}
   :response {}
   :slug nil})

(defn full-head-avoidance
  [jetty]
  (doseq [connector (.getConnectors jetty)]
    (.setRequestHeaderSize connector 8388608)))

(defn content-list [slug params]
  (model/find-all slug params))

(defn content-item [slug id params]
  (model/find-one slug (merge params {:where (db/clause "id:%1" [id])})))

(defn content-field [slug id field]
  ((content-item slug id {}) field))

(defn render
  "Prepare all various fields types for output as text."
  [slug content opts]
  (let [model ((keyword slug) @model/models)
        opts (update-in opts [:include] model/process-include)]
    (model/model-render model content opts)))

(defn render-field
  "Render a single content field to text, given the field type."
  [slug content field opts]
  (model/render (-> @model/models (keyword slug) :fields (keyword field)) content opts))

;; (defn process-include [include]
;;   (if (and include (not (empty? include)))
;;     (let [clauses (string/split include #",")
;;           paths (map #(string/split % #"\.") clauses)
;;           maps (reduce #(assoc %1 (keyword (first %2)) (process-include (string/join "." (rest %2)))) {} paths)]
;;       maps)
;;     {}))

;; formats -----------------------------------------------

(defn wrap-jsonp
  "Turn a callback name and a result into a jsonp response."
  [callback result]
  (str callback "(" result ")"))

(defn to-csv-column
  "Translate a list into something that can inhabit a single csv row."
  [bulk key]
  (let [morph (bulk (keyword key))]
    (cond
     (or (seq? morph) (vector? morph) (list? morph)) (string/join "|" (map #(str (last (first %))) morph))
     :else (str morph))))

(defn to-csv
  "Convert a bunch of data to CSV."
  [headings bulk]
  (csv/write-csv [(filter identity (map #(to-csv-column bulk %) headings))]))

(def prep-xml)

(defn prep-xml-item
  "Convert something into XML."
  [bulk]
  (map (fn [key] [key (prep-xml (bulk key))]) (keys bulk)))

(defn prep-xml
  "Convert everything into XML."
  [bulk]
  (cond
   (map? bulk) (prep-xml-item bulk)
   (or (seq? bulk) (vector? bulk) (list? bulk)) (map (fn [item]
                                                       [:item (prep-xml-item item)])
                                                       bulk)
   :else (str bulk)))

(def format-handlers
  {:json (fn [result params]
           (let [jsonify (generate-string result)
                 jsonp (params :jsonp)]
             (if jsonp
               (wrap-jsonp jsonp jsonify)
               jsonify)))
   :xml  (fn [result params]
           (let [xmlify (prep-xml result)]
             (with-out-str
               (xml/emit (xml/sexp-as-element [:api xmlify])))))
   :csv  (fn [result params]
           (let [bulk (result :response)
                 what (-> result :meta :type)
                 headings (if what (map name (keys (-> @model/models (keyword what) :fields))))
                 header (if what (csv/write-csv [headings]) "")]
             (cond
              (map? bulk) (str header (to-csv headings bulk))
              (or (seq? bulk) (vector? bulk) (list? bulk)) (apply str (cons header (map #(to-csv headings %) bulk))))))})

(defmacro action
  "Define an API action with the given slug and path."
  [slug path-args expr]
  `(defn ~slug [~(first path-args)]
     (log :action (str ~(name slug) " => " ~(first path-args)))
     ;; (if (any-role-granted? :admin)
       (let ~(vec (apply concat (map (fn [p] [`~p `(~(first path-args) ~(keyword p))]) (rest path-args))))
         (try
           (let [result# ~expr
                 format# (~(first path-args) :format)
                 handler# (or (format-handlers (keyword format#)) (format-handlers :json))]
             (handler# result# ~(first path-args)))
           (catch Exception e#
             (log :error (str "error rendering /" (string/join "/" [~@(rest path-args)]) ": "
                              (util/render-exception e#)))
             (generate-string
              ;; (json-str
              ~(reduce #(assoc %1 (keyword %2) %2) error path-args)))))
       ))
       ;; (generate-string
       ;;  (assoc error :meta {:status 403 :msg "you do not have access to this resource"})))))
       ;; (redirect "/permission-denied"))))

(defn wrap-response
  "Form a response from raw stuff."
  [response meta]
  {:meta (merge {:status "200" :msg "OK"} meta)
   :response response})

(defn ensure-seq
  "if given a map, convert to a seq containing only its values.
  otherwise, leave it alone"
  [col]
  (try
    (cond
     (map? col) (let [int-test (doall (map #(Integer/parseInt (name %)) (keys col)))]
                  (vals col))
     :else col)
    (catch Exception e
      col)))

(defn ensure-lists-in
  "flatten nested params into lists"
  [params]
  (reduce
   #(if (map? (%1 %2)) (assoc %1 %2 (ensure-seq (%1 %2))) %1)
   params
   (keys params)))

;; actions ------------------------------------------------

(action home [params]
  (wrap-response {} {}))

(defn slugify-filename
  [s]
  (.toLowerCase
   (string/replace
    (string/join "-" (re-seq #"[a-zA-Z0-9.]+" s))
    #"^[0-9]" "-")))

(defn upload
  "Handle a file upload over xdm."
  [params]
  (log :action (str "upload => " params))
  (let [upload (params :upload)
        slug (slugify-filename (:filename upload))
        asset (model/create
               :asset
               {:filename slug
                :content_type (:content-type upload)
                :size (:size upload)})
        dir (model/asset-dir asset)
        path (model/asset-path asset)
        response (str "
<!doctype html>
<html>
    <head>
    <title>upload response</title>
    </head>
    <body>
        <script type=\"text/javascript\">
            parent.rpc.returnUploadResponse({
                asset_id: " (asset :id) ",
                url: '" path "',
                context: '" (params :context) "',
            });
        </script>
    </body>
</html>
"
                )]
    (.mkdirs (io/file (util/pathify [(@config/app :asset-dir) dir])))
    (io/copy (-> params :upload :tempfile) (io/file (util/pathify [(@config/app :asset-dir) path])))
    response))

(defn find-by-params
  [params slug]
  (if ((keyword slug) @model/models)
    (let [include (params :include)
          order (or (params :order) "position asc")
          page_size (or (params :page_size) "30")
          page (Integer/parseInt (or (params :page) "1"))
          limit (Integer/parseInt (or (params :limit) page_size))
          offset (or (params :offset) (* limit (dec page)))
          where (params :where)
          included
          (merge
           params
           {:include include :limit limit :offset offset :order order :where where})
          _ (println included)
          found (model/find-all slug included)
          response (map #(render slug % included) found)
          showing (count response)
          total (db/tally slug)
          extra (if (> (rem total limit) 0) 1 0)
          total_pages (+ extra (quot total limit))]
      (wrap-response response {:type slug
                               :count showing
                               :total_items total
                               :total_pages total_pages
                               :page page
                               :page_size limit
                               :include include
                               :where where
                               :order order}))
    (merge error {:meta {:msg "no model by that name"}})))

(action list-all [params slug]
  (find-by-params params slug))

(action model-spec [params slug]
  (let [response (render "model" (first (db/query "select * from model where slug = '%1'" slug)) {:include {:fields {}}})]
    (wrap-response response {:type slug})))

(action item-detail [params slug id]
  (let [response (render slug (content-item slug id params) params)]
    (wrap-response response {:type slug})))

(action field-detail [params slug id field]
  (let [include (or {(keyword field) (params :include)} {})
        response (render-field slug (content-item slug id params) field (assoc params :include include))]
    (wrap-response response {})))

(action create-content [params slug]
  (let [response (render slug (model/create slug (ensure-lists-in (params (keyword slug)))) params)]
    (wrap-response response {:type slug})))

(action update-content [params slug id]
  (let [content (model/update slug id (ensure-lists-in (params (keyword slug))))
        response (render slug content params)]
    (wrap-response response {:type slug})))

(action delete-content [params slug id]
  (let [content (model/destroy slug id)
        response (render slug content params)]
    (wrap-response response {:type slug})))

(defn permission-denied [params]
  params)

;; (defn login [params]
;;   (let [account (first (model/rally :account {:where (str "email = '" (db/zap (params :email)) "'")}))
;;         crypted (account/crypt (params :password))]
;;     (if (and account (= crypted (account :crypted_password)))
;;       (do
;;         (session-put! :current-account account)
;;         ;; (str "bobobobobobob" (account :email)))
;;         (redirect "/"))
;;       (merge error {:error "login failed"}))))

;; ;; routes --------------------------------------------------

;; (defn authorize
;;   [request]
;;   (let [uri (:uri request)
;;         user (session-get :current-account)]
;;     (if user
;;       {:name (user :name) :roles #{:admin}}
;;       (do
;;         (session-put! :redirect-uri uri)
;;         (redirect "/permission-denied")))))

;; (def security-config
;;   [#"/login.*" :ssl
;;    #".*.css|.*.png" :any-channel
;;    #".*" :nossl])


;; ===============================
;; API app init
;; ===============================

(declare app)

(defn init
  "Initialize the API."
  []
  (config/init)
  (model/init)

  (defroutes main-routes
    ;; (route/files "/" {:root (@config/app :api-public)})
    (route/files "/" {:root (@config/app :asset-dir)})
    (route/resources "/")
    (GET  "/" {params :params} (home params))
    (POST "/upload" {params :params} (upload params))

    ;; (GET  "/permission-denied" {params :params} (permission-denied params))
    ;; (POST "/login" {params :params} (login params))
    (GET  "/:slug.:format" {params :params} (list-all params))
    (POST "/:slug.:format" {params :params} (create-content params))
    (GET  "/:slug/:id.:format" {params :params} (item-detail params))
    (PUT  "/:slug/:id.:format" {params :params} (update-content params))
    (DELETE  "/:slug/:id.:format" {params :params} (delete-content params))
    (GET  "/:slug/:id/:field.:format" {params :params} (field-detail params))

    (GET  "/:slug" {params :params} (list-all params))
    (POST "/:slug" {params :params} (create-content params))
    (GET  "/:slug/:id" {params :params} (item-detail params))
    (PUT  "/:slug/:id" {params :params} (update-content params))
    (DELETE  "/:slug/:id" {params :params} (delete-content params))
    (GET  "/:slug/:id/:field" {params :params} (field-detail params))

    (OPTIONS  "/" {params :params} (home params))
    (OPTIONS  "/upload" {params :params} (upload params))
    (OPTIONS  "/:slug" {params :params} (list-all params))
    (OPTIONS  "/:slug/:id" {params :params} (item-detail params))
    (OPTIONS  "/:slug/:id/:field" {params :params} (field-detail params))

    (route/not-found "NONONONONONON"))

  (if (@config/app :halo-enabled)
    (do
      (model/add-hook :page [:after_destroy :after_save] :halo-reload-routes api-halo/reload-routes)
      (model/add-hook :model [:after_destroy :after_save] :halo-reload-models api-halo/reload-models)))

  (def app
    (-> main-routes
        ;; (with-security authorize)
        handler/site
        ;; wrap-stateful-session
        (db/wrap-db @config/db)))
        ;; (with-secure-channel
        ;;   security-config
        ;;   (@config/app :api-port)
  ;;   (@config/app :api-ssl-port)))))

  (swank/start-server :host "127.0.0.1" :port 4009))

