(ns app.room
  (:import (java.util Date))
  (:require [clojure.core.async :refer [go <!]]
            [app.game-play :refer [play-game]]))

(def rooms (atom {}))

(def max-players 2)

(defn add-new-room [room-id public?]
  (if (get @rooms room-id)
    nil
    (let [new-room {:id room-id
                    :public public?
                    :created (Date.)
                    :num-players 0
                    :channels []
                    :running false}]
      (swap! rooms assoc room-id new-room)
      new-room)))

(defn is-full? [room]
  (= (:num-players room) max-players))

(defn start-room [room]
  (println "Room Started" "room-id:" (:id room))
  (apply play-game (:channels room))
  (println "Room Ended" "room-id:" (:id room)))

(defn join-room [room-id channel]
  (let [room (get @rooms room-id)]
    (if (and room (not (:running room)) (not (is-full? room)))
      (let [updated-room (-> room
                             (update :num-players inc)
                             (update :channels #(conj % channel)))]
        (swap! rooms assoc room-id updated-room)
        (when (is-full? updated-room)
          (swap! rooms assoc-in [room-id :running] true)
          (go (start-room updated-room)
              (swap! rooms assoc-in [room-id :running] false)))))))

(defn room-destroyer []
  (go
    (loop []
      (doseq [[room-id {:keys [running created]}] @rooms]
        (when (and
                (not running)
                (> (- (.getTime (java.util.Date.)) (.getTime created)) 30000))
            (swap! rooms dissoc room-id)))
      (Thread/sleep 10000)
      (recur))))