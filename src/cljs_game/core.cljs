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

(def canvas-width (.. canvas -width))
(def canvas-height (.. canvas -height))

(def app (.getElementById js/document "app"))

(def ctx (.getContext canvas "2d"))

(def pi (. js/Math -PI))

(def ball1 {:pos [20 20] :vel [10 0] :radius 15})

(def ball2 {:pos [40 20] :vel [0 10] :radius 15})

(def starting-state {:balls [ball1 ball2]})

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

(defn add-vector [v1 v2]
  (let [v1x (first v1) v1y (second v1)
        v2x (first v2) v2y (second v2)]
    [(+ v1x v2x) (+ v1y v2y)]))

(defn abs [n]
  (if (< n 0) (* -1 n) n))

(defn sq [n] (* n n))

(defn sqrt [n] (Math/sqrt n))

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

(defn new-vel [ov damp]
  (let [dir (if (< ov 0) -1 1)
        d (* dir damp)
        abs-ov (* dir ov)]
    (if (< abs-ov 0.07) 0 (- ov d))))

(defn temp-new-vel [ov damp]
  [(new-vel (first ov) damp) (new-vel (second ov) damp)])

(defn check-collision [b1 b2]
  (< (dis-vector (:pos b1) (:pos b2)) (+ (:radius b2) (:radius b1))))

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
    (assoc ball :vel (add-vector vel (-> dir (mul-vector vel) (scaler-mul-vector -2))))
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

(defn update-ball [{:keys [vel] :as ball}]
  (let [damp 0.00]
    (-> ball
        (update :pos #(new-pos % vel))
        (update :vel #(temp-new-vel % damp))
        (update-wall-hit))))

(defn update-state [state]
  (assoc state :balls (vec (map update-ball (:balls state)))))

(reset! world-state starting-state)

(js/setInterval (fn []
                  (reset! world-state (update-state @world-state))) 15)
