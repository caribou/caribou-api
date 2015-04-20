(ns caribou.api.controllers.home
  (:require [caribou.app.controller :refer [render]]
            [caribou.api.util :refer [split-format queries-to-map]]
            [caribou.model :as model]))

;;;;;;;;;;;;;;;;;;;;;;;;




(defn wrap-response
  [slug response]
  {:status 200 :body "200 : OK"
   :response response})


(defn home
  [request]
  (render
   :json
   {:whats :up}))



  
(defn index
  [request]
  (let [model-slug (-> request :params :model)
        [slug format] (split-format model-slug)
        opts (-> (:params request)
                 (queries-to-map)                 
                 (select-keys [:limit :offset :include :where :order]))
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
                 (select-keys [:limit :offset :include :where :order]))
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
            {:status 201 :body "201 : Created"})))


(defn update
  [request]
  (let [model-slug (-> request :params :model)
        [slug format] (split-format model-slug)
        spec (:params request)
        sani-spec (if-let [s-fn (::sanitize-fn request)]
                    (s-fn spec)
                    spec)]
    (if-not (:id spec)
      (render format
              {:status 400 :body "400 : Missing ID"})
      (let [updated-item (model/create slug sani-spec)]
        (render format (wrap-response slug updated-item))))))


(defn delete
  [request]
  (let [model-slug (-> request :params :model)
        [slug format] (split-format model-slug)
        opts (-> (:params request)
                 (queries-to-map)
                 (#(if-let [id (-> % :id)]
                     (assoc-in % [:where :id] id) %))
                 (select-keys [:limit :offset :include :where :order]))
        sani-opts (if-let [s-fn (::sanitize-fn request)]
                    (s-fn opts)
                    opts)
        items (model/gather slug sani-opts)]
    (doseq [item items :let [id (:id item)]]
      (model/destroy slug id))
    (render format
            {:status 204 :body "204 : Deleted"})))
