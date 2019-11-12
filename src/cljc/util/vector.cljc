(ns util.vector)

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
