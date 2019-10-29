(ns cljs-game.core
    (:require [reagent.core :as reagent :refer [atom]]))

(enable-console-print!)

;(println "This text is printed from src/figwheel-1/core.cljs. Go ahead and edit it and see reloading in action.")
;
;;; define your app data so that it doesn't get over-written on reload
;
;(defonce app-state (atom {:text "Hello world!"}))
;
;
;(defn hello-world []
;  [:div
;   [:h1 (:text @app-state)]
;   [:h3 "Edit this and watch it change!"]])
;
;(reagent/render-component [hello-world]
;                          (. js/document (getElementById "app")))
;
;(defn on-js-reload []
;  ;; optionally touch your app-state to force rerendering depending on
;  ;; your application
;  ;; (swap! app-state update-in [:__figwheel_counter] inc)
;)

;(ns hello.cruel-world)

(defn what-kind? []
  "Brilliantly Cruel")

(js/console.log (what-kind?))

(def canvas (.getElementById js/document "myCanvas"))

;(def canvas-width 480)
;(def canvas-height 480)

(def canvas-width (.. canvas -width))
(def canvas-height (.. canvas -height))

(def app (.getElementById js/document "app"))

(def ctx (.getContext canvas "2d"))

(def pi (. js/Math -PI))

(def ball1 {:id 1 :pos [20 20] :vel [2 2] :radius 15 :mass (* 15 15)})

(def ball2 {:id 2 :pos [90 20] :vel [0 0] :radius 15 :mass (* 15 15)})

(def ball3 {:id 3 :pos [50 90] :vel [0 0] :radius 15 :mass (* 15 15)})
;
;(def ball4 {:id 4 :pos [90 20] :vel [1 1] :radius 15 :mass (* 15 15)})

(def starting-state {:balls [ball1 ball2 ball3]})

(defonce world-state (atom starting-state))

(defn draw-ball [ctx {:keys [pos radius]}]
  ;(js/console.log "draw-ball called")
  (let [[posx posy] pos]
    (.beginPath ctx)
    (.arc ctx posx posy radius 0 (* 2 pi))
    (set! (.. ctx -fillStyle) "#FF0000")
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

;(defn new-vel [ov damp]
;  (let [dir (if (< ov 0) -1 1)
;        d (* dir damp)
;        abs-ov (* dir ov)]
;    (if (< abs-ov 0.07) 0 (- ov d))))
;
;(defn temp-new-vel [ov damp]
;  [(new-vel (first ov) damp) (new-vel (second ov) damp)])

(defn ball-side [{:keys [pos radius]} dir]
  (let [[px py] pos]
    (add-vector pos (scaler-mul-vector dir radius))))

(defn ball-left [ball] (ball-side ball [-1 0]))
(defn ball-right [ball] (ball-side ball [1 0]))
(defn ball-up [ball] (ball-side ball [0 1]))
(defn ball-down [ball] (ball-side ball [0 -1]))

(defn check-out-canvas [[posx posy]]
  (not (and (> posx 0) (< posx canvas-width) (> posy 0) (< posy canvas-height))))

(defn update-wall-hit-dir [{:keys [vel] :as ball} dir]
  (if (check-out-canvas (ball-side ball dir))
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
  (let [f (fn [a] (if (> (abs a) 0.001) (* a damp) 0))]
    [(f vx) (f vy)]))

(defn update-ball-vecs [{:keys [vel] :as ball}]
  (let [damp 1]
    (-> ball
        (update :pos #(new-pos % vel))
        (update :vel #(new-vel % damp)))))

(defn update-ball [{:keys [vel] :as ball} state]
  (let [damp 1]
    (-> ball
        (update-ball-collision (:balls state))
        (update :pos #(new-pos % vel))
        (update :vel #(new-vel % damp))
        (update-wall-hit))))

(defn update-balls [balls]
  ;(js/console.log (str balls))
  (let [b (doall (map update-ball-vecs balls))
        b (map #(update-ball-collision % b) b)
        b (map update-wall-hit b)]
    (vec b)))

(defn update-state [state]
  (assoc state :balls (update-balls (:balls state))))

(reset! world-state starting-state)

(js/setInterval (fn []
                  (reset! world-state (update-state @world-state))) 15)

;(def init-action {:pressed? false
;                  :start-pos [0 0]
;                  :end-pos [0 0]})
;
;(def action (atom init-action))
;
;(defn pos-in-ball? [pos ball]
;  (<= (abs-vector (sub-vector pos (:pos ball))) (:radius ball)))
;
;(defn apply-action-ball [action ball]
;  (let [{s :start-pos e :end-pos} action]
;    (if (pos-in-ball? s ball)
;      (assoc ball :vel (scaler-mul-vector (sub-vector s e) 0.01))
;      ball)))
;
;;(defn apply-action-state [action state]
;;  (let [balls (:balls state)]
;;    (loop [updated-balls '() rest-balls balls]
;;      (if (first rest-balls)
;;        (recur )))))
;
;(defn update-action [mouse-pos]
;  (if (not (:pressed? @action))
;    (do
;      (swap! action update :pressed? #(not %))
;      (swap! action assoc :start-pos mouse-pos))
;    (do
;      (swap! action update :pressed? #(not %))
;      (swap! action assoc :end-pos mouse-pos))))
