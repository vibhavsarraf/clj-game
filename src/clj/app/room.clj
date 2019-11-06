(ns app.room
  (:import (java.util Date)))

(def rooms (atom {}))

(def max-players 2)

(defn add-new-room [room-id public?]
  (if (get @rooms room-id)
    nil
    (let [new-room {:id room-id
                    :public public?
                    :created (Date.)
                    :num-players 0
                    :channels []}]
      (swap! rooms assoc room-id new-room)
      new-room)))

(defn is-full? [room]
  (= (:num-players room) max-players))

(defn start-room [room]
  (println "Room Started" "room-id:" (:id room)))

(defn join-room [room-id channel]
  (let [room (get @rooms room-id)]
    (if (and room (not (is-full? room)))
      (let [updated-room (-> room
                             (update :num-players inc)
                             (update :channels #(conj % channel)))]
        (swap! rooms assoc room-id updated-room)
        (if (is-full? updated-room) (start-room updated-room))))))
