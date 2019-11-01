(ns app.main
  (:require
    [org.httpkit.server :as server]
    [app.routes :as routes])
  (:gen-class))

(defn -main [& args]
  (println "Server starting...")
  (server/run-server #'routes/app
                     {:port 3449}))
