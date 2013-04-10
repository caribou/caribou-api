(ns caribou.api.routes)

(def api-routes
  '({:path "" :slug "api.home" :position nil :template "home.html" :method "" :action "home" :name "Home" :controller "home" :children
     ({:path ":model" :slug "api.index" :position nil :template "index.html" :method "GET" :action "index" :name "Index" :controller "home" :children
       ({:path ":id" :slug "api.detail" :position nil :template "detail.html" :method "GET" :action "detail" :name "Detail" :controller "home" :children ()}
        {:path ":id" :slug "api.update" :position nil :template "update.html" :method "PUT" :action "update" :name "Update" :controller "home" :children ()}
        {:path ":id" :slug "api.delete" :position nil :template "delete.html" :method "DELETE" :action "delete" :name "Delete" :controller "home" :children ()})}
      {:path ":model" :slug "api.create" :position nil :template "create.html" :method "POST" :action "create" :name "Create" :controller "home" :children ()})}))
