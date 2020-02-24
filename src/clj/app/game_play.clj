(ns app.game-play
  (:require [org.httpkit.server :refer :all]
            [cheshire.core :refer :all]
            [clojure.core.async :refer [<!! >!! alt!! go chan]]
            [game.soccer :as soccer])
  (:import (clojure.lang PersistentArrayMap)))

(defn conv-key-atoms [ma]
  (if (= (type ma) PersistentArrayMap)
    (reduce #(assoc %1 (-> %2 first keyword) (-> %2 second conv-key-atoms)) {} ma)
    ma))

(defn json->clj [json-str]
  (-> json-str parse-string conv-key-atoms))

(defn action-format-valid? [action]
  (every? action [:s :e]))

(defn get-final-state [state]
  (loop [s state]
    (if (:stable? s)
      s
      (recur (soccer/update-state s)))))

(defn action-handler [json-data my-ch op-ch state]
  (let [action (json->clj json-data)
        cur-state @state]
    (println "Got action from player:" action)
    (println "Current state: " cur-state)
    (when (soccer/action-valid? action cur-state)
      (send! op-ch (->> action generate-string (str "action#")))
      (let [new-state (get-final-state (soccer/apply-action-state action cur-state (fn [action])))
            send-final-state-fn #(send! % (->> new-state generate-string (str "finalstate#")))]
        (println "New state: " new-state)
        (doall (map send-final-state-fn [my-ch op-ch]))
        (reset! state new-state)))))

(defn play-game [ch1 ch2]
  (try
    (send! ch1 (str "startgame#" (generate-string {:player 1})))
    (send! ch2 (str "startgame#" (generate-string {:player 2})))
    (let [p1-ch (chan 1)
          p2-ch (chan 1)
          close-ch (chan 1)
          state (atom (assoc soccer/starting-state :game-started? true))]
      (reset! state (get-final-state @state))
      (println "Stable state: " @state)
      (on-receive ch1 #(>!! p1-ch %))
      (on-receive ch2 #(>!! p2-ch %))
      (on-close ch1 #(>!! close-ch %))
      (on-close ch2 #(>!! close-ch %))
      (loop []
        (alt!!
          p1-ch ([v] (do
                       (action-handler v ch1 ch2 state)
                       (recur)))
          p2-ch ([v] (do
                       (action-handler v ch2 ch1 state)
                       (recur)))
          close-ch ())))
    (catch Exception e (println "Game crashed " (.getMessage e)))))