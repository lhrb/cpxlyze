(ns cpxlyze.coupling
  (:require
   [clojure.math.combinatorics :as combo]))

(defn- name-pair-combinations [coll]
  (let [pair-combi (fn [m] (combo/combinations m 2))]
    (->> coll (map :filename) pair-combi)))

(defn change-coupling-links [log]
  (->> log
      (map :changes)
      (reduce (fn [acc elem]
                (into acc (name-pair-combinations elem))))
      (frequencies)
      (reduce-kv (fn [acc k v]
                    (conj acc {:source (first k)
                               :target (second k)
                               :value v}))
                 '())))

(defn add-index [coll]
  (map (fn [e idx]
         (assoc e :index idx)) coll (range (count coll))))

(defn sum-of-coupling [log]
 (->> log
      (map :changes)
      (remove #(<= (count %) 1))
      (map #(map :filename %))
      (reduce (fn [acc elem]
                (let [amount (dec (count elem))]
                  (->> elem
                       (reduce (fn [a b]
                                 (update-in a [b] (fnil #(+ % amount) 0)))
                               acc))))
              {})
      (reduce-kv (fn [acc k v]
                   (conj acc {:name k :value v}))
                 '())
      (add-index)))


(defn create-lookup-table [coll]
  (->> coll
       (reduce (fn [acc elem]
                 (into acc {(:name elem) (:index elem)}))
               {})))

(defn link-data [nodes links]
  (let [lookup (create-lookup-table nodes)]
    (->> links
         (map (fn [e]
                (let [source-idx (get lookup (:source e))
                      target-idx (get lookup (:target e))]
                  (if (or (nil? source-idx) (nil? target-idx))
                    nil
                   (-> e
                       (assoc :source source-idx)
                       (assoc :target target-idx))))))
         (remove nil?))))

(comment
  (require '[cpxlyze.parsers.git :refer [get-log]])

  (def path "../code-maat/")

  (def log (get-log path))

  (let [lookup (-> (sum-of-coupling log) (add-index) (create-lookup-table))]
    (->> (change-coupling-links log)
         (map (fn [e]
                (-> e
                    (assoc :source (get lookup (:source e)))
                    (assoc :target (get lookup (:target e))))))))

  (get {"test/code_maat/end_to_end/empty.git" 94}
       "test/code_maat/end_to_end/empty.git")


)
