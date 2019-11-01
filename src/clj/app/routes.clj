(ns app.routes
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.util.response :as response]))

(defroutes app-routes
           (GET "/" req (response/content-type
                          (response/resource-response "public/index.html")
                          "text/html"))
           (GET "/user/:id" [id] (str "<h1>Hello " id)))

(def app
  (wrap-defaults app-routes site-defaults))