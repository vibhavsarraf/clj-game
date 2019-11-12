(ns app.routes
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults api-defaults]]
            [ring.util.response :as rr]
            [org.httpkit.server :refer :all]
            [cheshire.core :refer :all]
            [app.room :refer :all]))

(defn join-room-handler [room-id req]
  (with-channel req channel
                (when (websocket? channel)
                  (println "WebSock channel" "room-id: " room-id)
                  (join-room room-id channel))))

(defn with-json [resp]
  (rr/content-type resp "application/json"))

(defn convert-json [val]
  (-> val generate-string rr/response with-json))

(defn api-routes []
  (routes
    (GET "/echo" req (str req "\r\nHello"))
    (GET "/joinroom/:room-id" [room-id :as req] (join-room-handler room-id req))
    (POST "/room" [id public]
      (let [public? (java.lang.Boolean. public)]
        (println (str "POST " "/room" id public?))
        (if (add-new-room id public?)
          (convert-json {:success true})
          (convert-json {:success false :reason "Room Already Exists"}))))))

(defroutes app-routes
           (GET "/" req (rr/content-type
                          (rr/resource-response "public/index.html")
                          "text/html"))
           (GET "/user/:id" [id] (str "<h1>Helloo " id))
           (context "/api" [] (api-routes)))

(def app
  (wrap-defaults app-routes (assoc-in site-defaults [:security :anti-forgery] false)))
