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

(def app (.getElementById js/document "app"))

(def ctx (.getContext canvas "2d"))

(def pi (. js/Math -PI))

(def starting-state {:posx 20 :posy 20 :radius 15 :vx 20 :vy 10})

(defonce world-state (atom starting-state))

(defn draw-ball [ctx {:keys [posx posy radius]}]
  (.beginPath ctx)
  (.arc ctx posx posy radius 0 (* 2 pi))
  (set! (.. ctx -fillStyle) "#FF0000")
  (.fill ctx)
  (.closePath ctx))

(defn draw [ctx state]
  (.clearRect ctx 0 0 (. canvas -width) (. canvas -height))
  (draw-ball ctx state))

(draw-ball ctx @world-state)


(add-watch world-state :on-change (fn [_ _ _ n]
                                    (draw ctx n)))

(draw ctx @world-state)

(defn new-pos [op vel] (+ op vel))
(defn new-vel [ov damp]
  (let [dir (if (< ov 0) -1 1)
        d (* dir damp)
        abs-ov (* dir ov)]
    (if (< abs-ov 0.07) 0 (- ov d))))

(defn update-ball [{:keys [posx posy vx vy] :as ball}]
  (let [damp 0.06]
    (-> ball
        (update-in [:posx] #(new-pos % vx))
        (update-in [:vx] #(new-vel % damp))
        (update-in [:posy] #(new-pos % vy))
        (update-in [:vy] #(new-vel % damp)))))

(defn update-state [state]
  (update-ball state))

(js/setInterval (fn []
                  (reset! world-state (update-state @world-state))) 15)
