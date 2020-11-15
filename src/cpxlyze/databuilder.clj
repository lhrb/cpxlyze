(ns cpxlyze.databuilder)

(defn- extract-files [log]
  (->> log
       (map (fn [m] (->> (:changes m)
                         (map :filename))))
       flatten))

(defn revs-per-file
  "counts appearences per file in git log messages"
  [log]
  (->> log
       extract-files
       frequencies
       (sort-by second >)
       (map (fn [[url revs]] {:url url :revisions revs}))))

(defn normalize-revs
  "normalize revisions to the interval [0,1]"
  [log]
  (let [entities (-> log revs-per-file)
        revs (->> entities (map :revisions))
        max (->> revs (reduce max))
        min (->> revs (reduce min))
        normalize (fn [x] (double (/ (- x min) (- max min))))]
   (->> entities
        (map (fn [entry] (update entry :revisions normalize))))))

(defn merge-revs-loc [loc log]
  (let [revs (-> log normalize-revs)]
    (->> revs
         (map (fn [{url :url revisions :revisions}]
                (let [lines (get loc url)]
                  (if (nil? lines)
                    nil
                    {:url url
                     :revisions revisions
                     :code lines}))))
         (remove nil?))))

(defn update-map [f m]
  (reduce-kv (fn [m k v] (assoc m k (f v))) {} m))

(defn merge-author-effort [coll]
  (apply merge-with +
       (->> coll
            (map #(select-keys % [:added :deleted]))
            (map #(update-map (fn [v] (Integer/valueOf v))  %)))))

(defn effort-abs-per-author [filename log]
  (->> log
       (filter (fn [x] (some #(= filename (:filename %)) (:changes x))))
       (map (fn [m]
              (->> (:changes m)
                   (filter #(= filename (:filename %)))
                   first
                   (merge (select-keys m [:author])))))
       (group-by :author)
       (update-map #(merge-author-effort %))
       (map (fn [[key val]] {:author/name key
                             :author/effort-abs (apply + (vals val))}))))

(defn assoc-effort-rel [coll]
  (let [sum (->> coll
                 (map :author/effort-abs)
                 (reduce +))]
    (->> coll
         (map (fn [e]
                (assoc e
                       :author/effort-rel
                       (double (/ (:author/effort-abs e) sum))))))))

(defn add-author-effort [log coll]
  (->> coll
       (map (fn [m]
               (assoc m :authors
                      (-> (:url m)
                          (effort-abs-per-author log)
                          (assoc-effort-rel)))))))

(defn build-data [loc log]
  (->> log
       (merge-revs-loc loc)
       (add-author-effort log)))

(comment
  (require '[cpxlyze.parsers.cloc :refer [get-loc]])
  (require '[cpxlyze.parsers.git :refer [get-log]])

  (def path "../code-maat/")

  (def log (get-log path))
  (def loc (get-loc path))

  (build-data loc log)

)
