(ns game.soccer
  (:require [game.math :as math]))

(def field {:width 480
            :height 320
            :left-goal [0 120 30 80]
            :right-goal [450 120 30 80]})

(def ball1 {:id 1 :pos '(80 60) :vel '(2 2) :radius 15 :mass (* 15 15) :player 1 :color "#FF0000"})
(def ball2 {:id 2 :pos '(110 160) :vel '(0 0) :radius 15 :mass (* 15 15) :player 1 :color "#FF0000"})
(def ball3 {:id 3 :pos '(80 260) :vel '(0 0) :radius 15 :mass (* 15 15) :player 1 :color "#FF0000"})
(def ball4 {:id 4 :pos '(400 60) :vel '(1 1) :radius 15 :mass (* 15 15) :player 2 :color "#00FF00"})
(def ball5 {:id 5 :pos '(370 160) :vel '(1 1) :radius 15 :mass (* 15 15) :player 2 :color "#00FF00"})
(def ball6 {:id 6 :pos '(400 260) :vel '(1 1) :radius 15 :mass (* 15 15) :player 2 :color "#00FF00"})
(def king
  {:id 7 :pos '(240 160) :vel '(0 0) :radius 10 :mass (* 10 10) :player 0 :special? true :color "#0000FF"})

(def starting-state {:balls [ball1 ball2 ball3 ball4 ball5 ball6 king]
                     :stable? false
                     :player-turn 1
                     :my-player 1
                     :game-started? false
                     :player-1-score 0
                     :player-2-score 0})

(defn new-pos [op vel]
  (math/add-vector op vel))

(defn ball-side [{:keys [pos radius]} dir]
  (math/add-vector pos (math/scale-vector dir radius)))

(defn check-out-field? [[posx posy]]
  (not (and (> posx 0) (< posx (:width field)) (> posy 0) (< posy (:height field)))))

(defn update-wall-hit-dir [{:keys [vel] :as ball} dir]
  (if (check-out-field? (ball-side ball dir))
    (assoc ball :vel (math/add-vector vel (-> dir
                                              (math/mul-vector (vec (map math/abs vel)))
                                              (math/scale-vector -2))))
    ball))

(def left-dir [-1 0])
(def right-dir [1 0])
(def up-dir [0 -1])
(def down-dir [0 1])

(defn update-wall-hit [ball]
  (reduce update-wall-hit-dir ball [left-dir right-dir up-dir down-dir]))

(defn check-collision [b1 b2]
  (<= (math/dis-vector (:pos b1) (:pos b2)) (+ (:radius b2) (:radius b1))))

(defn update-ball-vel-collision [b1 b2]
  (if (check-collision b1 b2)
    (do
      (let [{v1 :vel p1 :pos m1 :mass} b1
            {v2 :vel p2 :pos m2 :mass} b2
            xdif (math/sub-vector p1 p2)
            temp1 (/
                    (math/dot-vector (math/sub-vector v1 v2) xdif)
                    (math/sq (math/abs-vector xdif)))
            temp2 (/ (* 2 m2) (+ m1 m2))
            tempv (math/scale-vector xdif (* temp1 temp2))]
        (assoc b1 :vel (math/sub-vector v1 tempv))))
    b1))

(defn update-ball-collision [b balls]
  (let [update-func (fn [b1 b2]
                      (if (= (:id b1) (:id b2))
                        b1
                        (update-ball-vel-collision b1 b2)))]
    (reduce update-func b balls)))

(def damp 0.98)

(defn new-vel [[vx vy] damp]
  (let [f (fn [a] (if (> (math/abs a) 0.01) (* a damp) 0))]
    [(f vx) (f vy)]))

(defn update-ball-vecs [{:keys [vel] :as ball}]
  (-> ball
      (update :pos #(new-pos % vel))
      (update :vel #(new-vel % damp))))

(defn update-balls [balls]
  (let [b (map update-ball-vecs balls)
        b (map #(update-ball-collision % b) b)
        b (map update-wall-hit b)]
    (vec b)))

(defn balls-stable? [balls]
  (let [ball-stable? #(= [0 0] (:vel %))]
    (every? ball-stable? balls)))

;; Score

(defn get-king [state]
  (last (:balls state)))

(defn is-king-in-goal [state goal]
  (let [{[px py] :pos rad :radius} (get-king state)
        [x y w h] goal]
    (and (> px (+ x rad))
         (< px (- (+ x w) rad))
         (> py (+ y rad))
         (< py (- (+ y h) rad)))))

(defn update-score [state]
  (let [update-score-fn (fn [score-key]
                          (-> state
                              (update score-key inc)
                              (assoc :balls (:balls starting-state))))]
    (cond
      (is-king-in-goal state (:left-goal field)) (update-score-fn :player-2-score)
      (is-king-in-goal state (:right-goal field)) (update-score-fn :player-1-score)
      :else state)))

(defn update-state [state]
  (if (:stable? state)
    state
    (let [updated-state (-> state
                            (update :balls update-balls)
                            update-score)
          is-stable-now? (balls-stable? (:balls updated-state))]
      (assoc updated-state :stable? is-stable-now?))))

;; Action

(defn coord-in-ball? [pos ball]
  (<= (math/abs-vector (math/sub-vector pos (:pos ball))) (:radius ball)))

(defn apply-action-ball [action ball]
  (let [{s :start-pos e :end-pos} action]
    (if (coord-in-ball? s ball)
      (assoc ball :vel (math/scale-vector (math/sub-vector e s) 0.1))
      ball)))

(defn get-ball-moved-ind [balls action]
  (let [num-balls (count balls)
        s (:start-pos action)]
    (loop [x 0]
      (if (< x num-balls)
        (if (coord-in-ball? s (get balls x))
          x
          (recur (inc x)))
        nil))))

(defn my-turn? [state]
  (= (:my-player state) (:player-turn state)))

(defn action-valid? [action {:keys [balls player-turn game-started? stable?] :as state}]
  (println player-turn)
  (let [moved-index (get-ball-moved-ind balls action)]
    (and
      (not= (:start-pos action) (:end-pos action))
      (not= moved-index nil)
      stable?
      game-started?
      (= player-turn (-> balls (get moved-index) :player))
      )))

(defn apply-action-state [action state on-action-fn]
  (if (action-valid? action state)
    (do
      (println "Valid action")
      (on-action-fn action)
      (let [ball-moved-index (get-ball-moved-ind (:balls state) action)
            updated-state (update-in state [:balls ball-moved-index] #(apply-action-ball action %))
            is-stable-now? (balls-stable? (:balls updated-state))]
        (-> updated-state
            (assoc :stable? is-stable-now?)
            (update :player-turn #(bit-xor % 3)))))
    state))
