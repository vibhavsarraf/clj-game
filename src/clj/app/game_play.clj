(ns app.game-play
  (:require [org.httpkit.server :refer :all]))

(defn play-game [ch1 ch2]
  (send! ch1 "Hello from Game Server")
  (send! ch2 "Hello from Game Server")
  (Thread/sleep 10000))