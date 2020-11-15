(ns cpxlyze.statistics
  (:require [clojure.math.numeric-tower :as math]))

(defn square [x]
  (math/expt x 2))

(defn square-deviation [x mean]
  (square (- x mean)))

(defn mean [xs]
  (/ (reduce + xs)
     (count xs)))

(defn variance [xs]
  (let [m (mean xs)]
    (mean (map #(square-deviation % m) xs))))

(defn std-deviation [xs]
  (math/sqrt (variance xs)))
