(ns cljs-game.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as reagent :refer [atom]]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]))

(enable-console-print!)

(def canvas (.getElementById js/document "myCanvas"))

;(def canvas-width 480)
;(def canvas-height 320)

(def canvas-width (.. canvas -width))
(def canvas-height (.. canvas -height))

(def app (.getElementById js/document "app"))

(def ctx (.getContext canvas "2d"))

(def pi (. js/Math -PI))
(def damp 0.98)

(def ball1 {:id 1 :pos [20 20] :vel [2 2] :radius 15 :mass (* 15 15) :player 1 :color "#FF0000"})

(def ball2 {:id 2 :pos [90 20] :vel [0 0] :radius 15 :mass (* 15 15) :player 1 :color "#FF0000"})

(def ball3 {:id 3 :pos [50 200] :vel [0 0] :radius 15 :mass (* 15 15) :player 1 :color "#FF0000"})
;
(def ball4 {:id 4 :pos [100 90] :vel [1 1] :radius 15 :mass (* 15 15) :player 2 :color "#00FF00"})

(def ball5 {:id 5 :pos [120 20] :vel [1 1] :radius 15 :mass (* 15 15) :player 2 :color "#00FF00"})

(def ball6 {:id 6 :pos [200 20] :vel [1 1] :radius 15 :mass (* 15 15) :player 2 :color "#00FF00"})

(def special-ball {:id 7 :pos [200 200] :vel [1 1] :radius 10 :mass (* 10 10) :player 0 :special? true :color "#0000FF"})

(def starting-state {:balls []
                     ;:balls [ball1 ball2 ball3 ball4 ball5 ball6 special-ball]
                     :stable? false
                     :player-turn 1})

(defonce world-state (atom starting-state))

(defn draw-ball [ctx {:keys [pos radius color]}]
  ;(js/console.log "draw-ball called")
  (let [[posx posy] pos]
    (.beginPath ctx)
    (.arc ctx posx posy radius 0 (* 2 pi))
    (set! (.. ctx -fillStyle) color)
    (.fill ctx)
    (.closePath ctx)))

(defn draw [ctx state]
  ;(js/console.log "draw called" (str state))
  (.clearRect ctx 0 0 (. canvas -width) (. canvas -height))
  (doall (map (partial draw-ball ctx) (:balls state))))

;(draw-ball ctx @world-state)

(add-watch world-state :on-change (fn [_ _ _ n]
                                    (draw ctx n)))

;(draw ctx @world-state)

(js/console.log "drawing world-state")

(defn abs [n]
  (if (< n 0) (* -1 n) n))

(defn sq [n] (* n n))

(defn sqrt [n] (Math/sqrt n))

(defn add-vector [v1 v2]
  (let [[v1x v1y] v1
        [v2x v2y] v2]
    [(+ v1x v2x) (+ v1y v2y)]))

(defn sub-vector [v1 v2]
  (let [[v1x v1y] v1
        [v2x v2y] v2]
    [(- v1x v2x) (- v1y v2y)]))

(defn dot-vector [v1 v2]
  (let [[v1x v1y] v1
        [v2x v2y] v2]
    (+ (* v1x v2x) (* v1y v2y))))

(defn abs-vector [v]
  (let [vx (first v) vy (second v)]
    (sqrt (+ (sq vx) (sq vy)))))

(defn mul-vector [[v1x v1y] [v2x v2y]]
  [(* v1x v2x) (* v1y v2y)])

(defn scaler-mul-vector [[vx vy] a]
  [(* a vx) (* a vy)])

(defn neg-vector [v]
  [(* -1 (first v)) (* -1 (second v))])

(defn dis-vector [v1 v2]
  (abs-vector (add-vector v1 (neg-vector v2))))

(defn new-pos [op vel]
  (add-vector op vel))

(defn ball-side [{:keys [pos radius]} dir]
  (add-vector pos (scaler-mul-vector dir radius)))

(defn ball-left [ball] (ball-side ball [-1 0]))
(defn ball-right [ball] (ball-side ball [1 0]))
(defn ball-up [ball] (ball-side ball [0 1]))
(defn ball-down [ball] (ball-side ball [0 -1]))

(defn check-out-canvas? [[posx posy]]
  (not (and (> posx 0) (< posx canvas-width) (> posy 0) (< posy canvas-height))))

(defn update-wall-hit-dir [{:keys [vel] :as ball} dir]
  (if (check-out-canvas? (ball-side ball dir))
    (assoc ball :vel (add-vector vel (-> dir (mul-vector (let [[vx vy] vel] [(abs vx) (abs vy)])) (scaler-mul-vector -2))))
    ball))

(def left-dir [-1 0])
(def right-dir [1 0])
(def up-dir [0 -1])
(def down-dir [0 1])

(defn update-wall-hit [ball]
  (-> ball
      (update-wall-hit-dir left-dir)
      (update-wall-hit-dir right-dir)
      (update-wall-hit-dir up-dir)
      (update-wall-hit-dir down-dir)))

(defn modify-state []
  (reset! world-state starting-state))

(defn check-collision [b1 b2]
  (<= (dis-vector (:pos b1) (:pos b2)) (+ (:radius b2) (:radius b1))))

(defn update-ball-vel-collision [b1 b2]
  (if (check-collision b1 b2)
    (do
      ;(js/console.log "collision detected")
      (let [{v1 :vel p1 :pos m1 :mass} b1
            {v2 :vel p2 :pos m2 :mass} b2
            xdif (sub-vector p1 p2)
            temp1 (/
                    (dot-vector (sub-vector v1 v2) xdif)
                    (let [a (abs-vector xdif)]
                      (sq a)))
            temp2 (/ (* 2 m2) (+ m1 m2))
            tempv (scaler-mul-vector xdif (* temp1 temp2))]
        (assoc b1 :vel (sub-vector v1 tempv))))
    b1))

(defn update-ball-collision [b balls]
  (loop [b1 b b2 (first balls) rest-balls (rest balls)]
    (if b2
      (if (= (:id b1) (:id b2))
        (recur b1 (first rest-balls) (rest rest-balls))
        (recur (update-ball-vel-collision b1 b2) (first rest-balls) (rest rest-balls)))
      b1)))

(defn new-vel [[vx vy] damp]
  (let [f (fn [a] (if (> (abs a) 0.01) (* a damp) 0))]
    [(f vx) (f vy)]))

(defn update-ball-vecs [{:keys [vel] :as ball}]
  (-> ball
      (update :pos #(new-pos % vel))
      (update :vel #(new-vel % damp))))

;(defn update-ball [{:keys [vel] :as ball} state]
;  (let [damp 0.8]
;    (-> ball
;        (update-ball-collision (:balls state))
;        (update :pos #(new-pos % vel))
;        (update :vel #(new-vel % damp))
;        (update-wall-hit))))

(defn update-balls [balls]
  ;(js/console.log (str balls))
  (let [b (doall (map update-ball-vecs balls))
        b (map #(update-ball-collision % b) b)
        b (map update-wall-hit b)]
    (vec b)))

(defn balls-stable? [balls]
  (let [ball-stable? (fn [{:keys [vel]}]
                       (= 0.0 (abs-vector vel)))]
    (every? #(ball-stable? %) balls)))

(defn update-state [{:keys [balls stable?] :as state}]
  (if stable?
    state
    (let [updated-balls (update-balls balls)]
      (-> state
          (assoc :balls updated-balls)
          (assoc :stable? (balls-stable? updated-balls))))))

(def init-action {:pressed? false
                  :start-pos [0 0]
                  :end-pos [0 0]})

(def action (atom init-action))

(defn pos-in-ball? [pos ball]
  (<= (abs-vector (sub-vector pos (:pos ball))) (:radius ball)))

(defn apply-action-ball [action ball]
  (let [{s :start-pos e :end-pos} action]
    (if (pos-in-ball? s ball)
      (assoc ball :vel (scaler-mul-vector (sub-vector s e) 0.05))
      ball)))

(defn get-ball-moved [balls action]
  (let [total-balls (count balls)
        s (:start-pos action)]
    (loop [x 0]
      ;(println x)
      (if (< x total-balls)
        (if (pos-in-ball? s (get balls x))
          x
          (recur (inc x)))
        nil))))

(defn change-player [cur-player]
  (if (= 1 cur-player) 2 1))

(defn apply-action-state [action state]
  (let [{balls :balls player-turn :player-turn} state
        ind (get-ball-moved balls action)
        ball-moved (get balls ind)]
    (if (and (not= ind nil) (= player-turn (:player ball-moved)))
      (-> state
          (assoc-in [:balls ind] (apply-action-ball action ball-moved))
          (assoc :stable? false)
          (update :player-turn change-player))
      state)))

(set! (.. js/document -body -onmousedown) (fn [e]
                                 (let [mouse-pos [(.. e -x) (.. e -y)]]
                                   ;(js/console.log e)
                                   (swap! action assoc :start-pos mouse-pos))))

(set! (.. js/document -body -onmouseup) (fn [e]
                                          (if (:stable? @world-state)
                                            (let [mouse-pos [(.. e -x) (.. e -y)]]
                                              ;(js/console.log e)
                                              (swap! action assoc :end-pos mouse-pos)
                                              (swap! world-state #(apply-action-state @action %))))))

(defonce screen-loaded? (atom false))

(when (not @screen-loaded?)
  (reset! screen-loaded? true)
  (reset! world-state starting-state)
  (js/setInterval (fn []
                    (reset! world-state (update-state @world-state))) 15))


;-----------------------------------------------------------------------------------
;Create and Join room

(defn get-element [id]
  (.getElementById js/document id))

(def create-form (.getElementById js/document "create-room-form"))
(def create-room-id-input (.getElementById js/document "create-room-id"))

(def join-form (.getElementById js/document "join-room-form"))
(def join-room-id-input (.getElementById js/document "join-room-id"))

(defn create-room-request [id public?]
  (go (let [response (<! (http/post "http://localhost:3000/api/room"
                                   {:with-credentials? false
                                    :form-params {:id id :public true}}))]
        (prn (:status response))
        (prn (:body response)))))

(def socket (atom nil))

(defn join-room [id]
  (reset! socket (new js/WebSocket (str "ws://localhost:3000/api/joinroom/" id)))
  (set! (.-innerHTML (get-element "create-room")) "")
  (set! (.-innerHTML (get-element "message")) "Waiting for Opponent"))

(set! (.-onsubmit create-form) (fn [e]
                          (.preventDefault e)
                          (let [id (.-value create-room-id-input)]
                            (go (<! (create-room-request id true))
                                (join-room id))
                            )))

(set! (.-onsubmit join-form) (fn [e]
                                 (.preventDefault e)
                                 (let [id (.-value join-room-id-input)]
                                   ;(go (<! (create-room-request id true)))
                                   (join-room id)
                                   )))

