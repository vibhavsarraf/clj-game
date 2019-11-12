(ns app.game-play
  (:require [org.httpkit.server :refer :all]
            [cheshire.core :refer :all]
            [clojure.core.async :refer [<!! >!! alt!! go chan]])
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
  (let [p1-ch (chan 1)
        p2-ch (chan 1)
        close-ch (chan 1)]
    (on-receive ch1 #(>!! p1-ch %))
    (on-receive ch2 #(>!! p2-ch %))
    (on-close ch1 #(>!! close-ch %))
    (on-close ch2 #(>!! close-ch %))
    (loop []
      (alt!!
        p1-ch ([v] (do
                     (player-handler v ch1 ch2)
                     (recur)))
        p2-ch ([v] (do
                     (player-handler v ch2 ch1)
                     (recur)))
        close-ch ()))))