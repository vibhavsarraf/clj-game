(ns cljs-game.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as reagent :refer [atom]]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]
            [game.soccer :as soccer]
            [game.math :as math]))

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

(defonce world-state (atom soccer/starting-state))

(.clearRect ctx 0 0 canvas-width canvas-height)

(defn fill-rect [a b c d]
  (.fillRect ctx a b c d))

(def field (merge soccer/field {:goal-color "pink"
                                :background "grey"}))

(defn draw-field [ctx field]
  (set! (.-fillStyle ctx) (:background field))
  (.fillRect ctx 0 0 (:width field) (:height field))
  (set! (.-fillStyle ctx) (:goal-color field))
  (apply fill-rect (:left-goal field))
  (apply fill-rect (:right-goal field)))

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

(draw ctx @world-state)

(add-watch world-state :on-change (fn [_ _ _ n]
                                    (draw ctx n)))

(defn on-stable-msg [{:keys [my-player player-turn game-started?] :as state}]
  (when game-started?
    (set-message! (str (if (= my-player player-turn) "Your Turn. " "Opponent's Turn. ")
                       (if (= my-player 1) "You are red." "You are green.")))))

(def init-action {:pressed? false
                  :start-pos [0 0]
                  :end-pos [0 0]})

(def action (atom init-action))

(defn clj->json [data]
  (-> data
      clj->js
      js/JSON.stringify))

(set! (.. js/document -body -onclick) (fn [e]
                                        (println "Got click")
                                        (println @action)
                                        (if (:pressed? @action)
                                          (when (soccer/my-turn? @world-state)
                                            (let [mouse-pos [(.. e -x) (.. e -y)]
                                                  on-action-fn (fn [action]
                                                                 (println "Sending action: " action)
                                                                 (when @socket (.send @socket (clj->json action))))]
                                              (swap! action assoc :end-pos mouse-pos :pressed? false)
                                              (swap! world-state #(soccer/apply-action-state @action % on-action-fn))))
                                          (let [mouse-pos [(.. e -x) (.. e -y)]
                                                is-pos-in-ball? (fn [{:keys [pos radius]}]
                                                                  (< (math/dis-vector mouse-pos pos) radius))]
                                            (when (some is-pos-in-ball? (:balls @world-state))
                                              (swap! action assoc :start-pos mouse-pos :pressed? true))))))

(defonce screen-loaded? (atom false))

(defonce final-state (atom nil))

(when (not @screen-loaded?)
  (reset! screen-loaded? true)
  (reset! world-state soccer/starting-state)
  (js/setInterval (fn []
                    (let [state @world-state]
                      (when (not (:stable? state))
                        (let [new-state (soccer/update-state state)]
                          (reset! world-state new-state)
                          (when (:stable? new-state)
                            (println "Stable state: " new-state)
                            (println "State from server: " @final-state)
                            (on-stable-msg new-state))))))
                  15))


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

(defn conv-key-atoms [ma]
  (cond
    (= (type ma) cljs.core/PersistentArrayMap) (reduce #(assoc %1 (-> %2 first keyword) (-> %2 second conv-key-atoms)) {} ma)
    (= (type ma) cljs.core/PersistentVector) (reduce #(conj %1 (conv-key-atoms %2)) [] ma)
    :else ma))

(defn json->clj [json-str] (-> json-str
                                     js/JSON.parse
                                     js->clj
                                     conv-key-atoms
                                     ))

(defn start-game [player sync-state]
  (reset! world-state soccer/starting-state)
  (swap! world-state assoc :my-player player :game-started? true)
  (set-message! (str "Game Started." (if (= player 1) "You are red." "You are green."))))

(defn on-socket-receive [data]
  ;(println "Got message from server:" data)
  (let [[msg json-str] (clojure.string/split data "#")
        sync-data (json->clj json-str)]
    (println "sync-data" sync-data)
    (case msg
      "startgame" (start-game (get sync-data :player) (get sync-data :sync))
      "action" (let [action sync-data]
                 (swap! world-state #(soccer/apply-action-state action % (fn [action]))))
      "finalstate" (reset! final-state data))))

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

