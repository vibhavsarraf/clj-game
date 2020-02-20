(ns cljs-game.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as reagent :refer [atom]]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]
            [game.soccer :as soccer]))

(enable-console-print!)

(def host (.. js/window -location -host))
(def href (.. js/window -location -href))

(def canvas (.getElementById js/document "myCanvas"))

;(def canvas-width 480)
;(def canvas-height 320)

(def canvas-width (.. canvas -width))
(def canvas-height (.. canvas -height))

(defn get-element [id]
  (.getElementById js/document id))

(defn set-message! [msg]
  (set! (.-innerHTML (get-element "message")) msg))

(def app (.getElementById js/document "app"))

(def ctx (.getContext canvas "2d"))

(def pi (. js/Math -PI))
(def damp 0.98)

(defonce socket (atom nil))

(def ball1 {:id 1 :pos [80 60] :vel [2 2] :radius 15 :mass (* 15 15) :player 1 :color "#FF0000"})

(def ball2 {:id 2 :pos [110 160] :vel [0 0] :radius 15 :mass (* 15 15) :player 1 :color "#FF0000"})

(def ball3 {:id 3 :pos [80 260] :vel [0 0] :radius 15 :mass (* 15 15) :player 1 :color "#FF0000"})
;
(def ball4 {:id 4 :pos [400 60] :vel [1 1] :radius 15 :mass (* 15 15) :player 2 :color "#00FF00"})

(def ball5 {:id 5 :pos [370 160] :vel [1 1] :radius 15 :mass (* 15 15) :player 2 :color "#00FF00"})

(def ball6 {:id 6 :pos [400 260] :vel [1 1] :radius 15 :mass (* 15 15) :player 2 :color "#00FF00"})

(def special-ball {:id 7 :pos [240 160] :vel [0 0] :radius 10 :mass (* 10 10) :player 0 :special? true :color "#0000FF"})

(def starting-state {:balls [ball1 ball2 ball3 ball4 ball5 ball6 special-ball]
                     :stable? false
                     :player-turn 1
                     :my-player 1
                     :game-started? false})

(defonce world-state (atom starting-state))

(.clearRect ctx 0 0 canvas-width canvas-height)

(def field {:width canvas-width
            :height canvas-height
            :goal-color "pink"
            :background "grey"})

(defn draw-field [ctx field]
  (set! (.-fillStyle ctx) (:background field))
  (.fillRect ctx 0 0 canvas-width canvas-height)
  (set! (.-fillStyle ctx) (:goal-color field))
  (.fillRect ctx 0 120 30 80)
  (.fillRect ctx 450 120 30 80))

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
  (draw-field ctx field)
  (doall (map (partial draw-ball ctx) (:balls state))))

(draw ctx starting-state)

(add-watch world-state :on-change (fn [_ _ _ n]
                                    (draw ctx n)))

;(defn update-ball-collision [b balls]
;  (loop [b1 b b2 (first balls) rest-balls (rest balls)]
;    (if b2
;      (if (= (:id b1) (:id b2))
;        (recur b1 (first rest-balls) (rest rest-balls))
;        (recur (update-ball-vel-collision b1 b2) (first rest-balls) (rest rest-balls)))
;      b1)))


(defn on-stable [{:keys [my-player player-turn game-started?] :as state}]
  (when game-started?
    (set-message! (if (= my-player player-turn) "Your Turn" "Opponent's Turn"))))

;(defn update-state [{:keys [balls stable?] :as state}]
;  (if stable?
;    state
;    (let [updated-balls (update-balls balls)
;          now-stable? (balls-stable? updated-balls)]
;      (when now-stable? (on-stable state))
;      (-> state
;          (assoc :balls updated-balls)
;          (assoc :stable? now-stable?)))))

(def init-action {:pressed? false
                  :start-pos [0 0]
                  :end-pos [0 0]})

(def action (atom init-action))



;(defn change-player [cur-player]
;  (if (= 1 cur-player) 2 1))
;
(defn clj->json [data]
  (-> data
      clj->js
      js/JSON.stringify))



;(defn apply-action-state [action state]
;  (println "Applying action" action)
;  (let [{balls :balls player-turn :player-turn game-started? :game-started?} state
;        ind (get-ball-moved balls action)
;        ball-moved (get balls ind)]
;    (if
;      (and
;        (not= ind nil)
;        (or (not game-started?) (= player-turn (:player ball-moved))))
;      (do
;        (when @socket (.send @socket (clj->json {:s (:start-pos action) :e (:end-pos action)})))
;        (-> state
;            (assoc-in [:balls ind] (apply-action-ball action ball-moved))
;            (assoc :stable? false)
;            (update :player-turn change-player)))
;      state)))

(set! (.. js/document -body -onmousedown) (fn [e]
                                 (let [mouse-pos [(.. e -x) (.. e -y)]]
                                   (swap! action assoc :start-pos mouse-pos))))

(set! (.. js/document -body -onmouseup) (fn [e]
                                          (println "Got mouse up event")
                                          (when (soccer/my-turn? @world-state)
                                            ;(let [{stable? :stable? game-started? :game-started?} @world-state])
                                            ;(if) (and
                                            ;       (:stable? @world-state)
                                            ;       (or (not game-started?) (my-turn? @world-state)))
                                            (let [mouse-pos [(.. e -x) (.. e -y)]
                                                  on-action-fn (fn [action]
                                                                 (when @socket (.send @socket (clj->json {:s (:start-pos action) :e (:end-pos action)}))))]
                                              ;(js/console.log e)
                                              (swap! action assoc :end-pos mouse-pos)
                                              (swap! world-state #(soccer/apply-action-state @action % on-action-fn))))))

(defonce screen-loaded? (atom false))

(when (not @screen-loaded?)
  (reset! screen-loaded? true)
  (reset! world-state starting-state)
  (js/setInterval (fn []
                    (reset! world-state (soccer/update-state @world-state))
                    (when (:stable? @world-state)
                      (on-stable @world-state))
                    ;(on-stable @world-state)
                    ) 15))


;-----------------------------------------------------------------------------------
;Create and Join room

(def create-form (.getElementById js/document "create-room-form"))
(def create-room-id-input (.getElementById js/document "create-room-id"))

(def join-form (.getElementById js/document "join-room-form"))
(def join-room-id-input (.getElementById js/document "join-room-id"))

(defn create-room-request [id public?]
  (go (let [response (<! (http/post (str href "api/room")
                                    {:with-credentials? false
                                    :form-params {:id id :public true}}))]
        response)))

(def socket-data (atom nil))

(defn conv-key-atoms [ma]
  (if (= (type ma) cljs.core/PersistentArrayMap)
    (reduce #(assoc %1 (-> %2 first keyword) (-> %2 second conv-key-atoms)) {} ma)
    ma))

(defn json->clj [json-str] (-> json-str
                                     js/JSON.parse
                                     js->clj
                                     conv-key-atoms
                                     ))

(defn start-game [player sync-state]
  (reset! world-state starting-state)
  (swap! world-state assoc :my-player player :game-started? true)
  (set-message! (str "Game Started." (if (= player 1) "You are red." "You are green."))))

(defn on-socket-receive [data]
  ;(println "Got message from server:" data)
  (let [[msg json-str] (clojure.string/split data "#")
        sync-data (json->clj json-str)]
    (println "sync-data" sync-data)
    (case msg
      "startgame" (start-game (get sync-data :player) (get sync-data :sync))
      "action" (let [action {:start-pos (get sync-data :s) :end-pos (get sync-data :e)}]
                 (swap! world-state #(soccer/apply-action-state action % (fn [action]))))
      "sync" (reset! socket-data data))))

(defn join-room [id]
  (let [ws (new js/WebSocket (str "ws://" host "/api/joinroom/" id))]
    (reset! socket ws)
    (set! (.-onmessage ws) (fn [e]
                             (js/console.log "GameServer: " (.-data e))
                             (on-socket-receive (.-data e)))))
  (set! (.-innerHTML (get-element "create-room")) "")
  (set-message! "Waiting for Opponent"))

(set! (.-onsubmit create-form) (fn [e]
                          (.preventDefault e)
                          (let [id (.-value create-room-id-input)]
                            (go (let [response (<! (create-room-request id true))]
                                  (if
                                    (and
                                      (= (:status response) 200)
                                      (= (get-in response [:body :success]) true))
                                    (join-room id)
                                    (set-message! (get-in response [:body :reason])))))
                            )))

(set! (.-onsubmit join-form) (fn [e]
                                 (.preventDefault e)
                                 (let [id (.-value join-room-id-input)]
                                   (join-room id)
                                   )))

