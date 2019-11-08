(ns app.game-play
  (:require [org.httpkit.server :refer :all]
            [cheshire.core :refer :all])
  (:import (clojure.lang PersistentArrayMap)))

(defn conv-key-atoms [ma]
  (if (= (type ma) PersistentArrayMap)
    (reduce #(assoc %1 (-> %2 first keyword) (-> %2 second conv-key-atoms)) {} ma)
    ma))

(defn json->clj [json-str]
  (-> json-str parse-string conv-key-atoms))


(defn player-handler [json-data my-ch op-ch]
  (let [data (json->clj json-data)]
    (println "Got data from player1:" data)
    (send! op-ch (->> data generate-string (str "action#")))))

(defn play-game [ch1 ch2]
  (send! ch1 (str "startgame#" (generate-string {:player 1})))
  (send! ch2 (str "startgame#" (generate-string {:player 2})))
  (on-receive ch1 #(player-handler % ch1 ch2))
  (on-receive ch2 #(player-handler % ch2 ch1))
  (Thread/sleep 10000))