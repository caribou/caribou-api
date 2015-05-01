(ns caribou.api.controllers.home
  (:require [caribou.app.controller :refer [render]]
            [caribou.api.util :refer [split-format queries-to-map]]
            [caribou.model :as model]))




(defn wrap-response
  "Create a response with a 'meta' field for consumption by api user."
  ([slug response]
   (wrap-response slug response 200 "OK"))
  ([slug response status msg]
   {:meta {:status status :msg msg :type (name slug)}
    :status status
    :response response}))


(defn home
  [request]
  (render
   :json
   {:whats :up}))


(defn options [request]
  (let [accepted-methods (if (-> request :params :id)
                           ["GET" "PUT" "DELETE"]
                           ["GET" "POST"])]
    {:status 200
     :body nil
     :headers {"Allow" (clojure.string/join ", "accepted-methods)}}))
  
(defn index
  [request]
  (let [model-slug (-> request :params :model)
        [slug format] (split-format model-slug)
        opts (-> (:params request)
                 (queries-to-map)                 
                 (select-keys [:limit :offset :include :where :order :fields]))
        sani-opts (if-let [s-fn (::sanitize-fn request)]
                    (s-fn opts)
                    opts)
        items (model/gather slug sani-opts)]
    (render format (wrap-response slug items))))


(defn detail
  "Detail acts like 'index', except it returns a single entry and
  it accepts an :id in the :params."
  [request]
  (let [model-slug (-> request :params :model)
        [slug format] (split-format model-slug)
        opts (-> (:params request)
                 (queries-to-map)
                 (#(if-let [id (-> % :id)]
                     (assoc-in % [:where :id] id) %))
                 (select-keys [:limit :offset :include :where :order :fields]))
        sani-opts (if-let [s-fn (::sanitize-fn request)]
                    (s-fn opts)
                    opts)
        item (first
              (model/gather slug sani-opts))]
    (render format (wrap-response slug item))))


(defn create
  [request]
  (let [model-slug (-> request :params :model)
        [slug format] (split-format model-slug)
        ;; do not allow IDs when creating an object
        spec (-> request :params (dissoc :id))
        sani-spec (if-let [s-fn (::sanitize-fn request)]
                    (s-fn spec)
                    spec)
        new-item (model/create slug sani-spec)]
    (render format
            (wrap-response slug new-item 201 "Created"))))


(defn update
  [request]
  (let [model-slug (-> request :params :model)
        [slug format] (split-format model-slug)
        spec (:params request)
        sani-spec (if-let [s-fn (::sanitize-fn request)]
                    (s-fn spec)
                    spec)]
    (if-not (:id spec)
      (render format (wrap-response slug nil 400 "Missing ID"))
      (let [updated-item (model/create slug sani-spec)]
        (render format (wrap-response slug updated-item))))))


(defn delete
  "Delete an item. MUST have an ID provided."
  [request]
  (let [provided-id (get-in request [:params :id])
        model-slug (-> request :params :model)
        [slug format] (split-format model-slug)
        opts (-> (:params request)
                 (queries-to-map)
                 ((fn [m] (assoc-in m [:where :id] provided-id)))
                 (select-keys [:limit :offset :include :where
                               :order :fields :id]))
        ;; allow the sanitize-fn to discard results
        sani-opts (if-let [s-fn (::sanitize-fn request)]
                    (s-fn opts)
                    opts)
        item (model/pick slug sani-opts)]
    (if provided-id
      (do (model/destroy slug (:id item))
          (render format
                  (wrap-response slug nil 204 "Deleted")))
      (render format
              (wrap-response slug nil 400 "Missing ID")))))
