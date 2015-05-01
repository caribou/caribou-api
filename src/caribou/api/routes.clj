(ns caribou.api.routes
  (:require [caribou.app.pages :as pages]))

(def api-routes
  [["/" :home {:ALL {:controller 'home :action 'home}}
    [[":model" :api.index {:GET {:controller 'home :action 'index}
                           :POST {:controller 'home :action 'create}
                           :OPTIONS {:controller 'home :action 'options}}
      [[":id" :api.item {:GET {:controller 'home :action 'detail}
                         :PUT {:controller 'home :action 'update}
                         :DELETE {:controller 'home :action 'delete}
                         :OPTIONS {:controller 'home :action 'options}}]]]]]])

(defn build-api-routes
  []
  (pages/bind-actions api-routes 'caribou.api.controllers))

