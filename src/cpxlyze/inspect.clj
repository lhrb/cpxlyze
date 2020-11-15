(ns cpxlyze.inspect)

(defn- extract-from-changes [k log]
  (->> log
       (map (fn [m] (->> (:changes m)
                         (map k))))
       flatten))

(defn- extract-files [log]
  (->> log
       (map (fn [m] (->> (:changes m)
                         (map :filename))))
       flatten))

(defn- count-entities [entities]
  (->> entities
       distinct
       count))

(defn- count-authors [log]
  (->> log (map :author) distinct count))

(defn revs-per-file
  "counts appearences per file in git log messages"
  [log]
  (->> log
       extract-files
       frequencies
       (sort-by second >)
       (map (fn [[url revs]] {:url url :revisions revs}))))

(defn added [log]
 (->> log
      (extract-from-changes :added)
      (map #(Integer/valueOf %))
      (reduce +)))

(defn deleted [log]
 (->> log
      (extract-from-changes :deleted)
      (map #(Integer/valueOf %))
      (reduce +)))

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

(defn summary [log]
  (let [entities (extract-files log)]
    {:commits (count log)
     :entities (count-entities entities)
     :entites-changed (count entities)
     :authors (count-authors log)
     :added (added log)
     :deleted (deleted log)}))

(defn merge-revs-loc [log loc]
  (let [revs (-> log normalize-revs)]
    (->> revs
         (map (fn [{url :url revisions :revisions}]
                (let [lines (get loc url)]
                  (if (nil? lines)
                    nil
                    {:url url
                     :revisions revisions
                     :code lines}))))
         (remove nil?)
         (map #(update % :url (fn [x] (str "root/" x)))))))


(comment
  (require '[cpxlyze.parsers.cloc :refer [get-loc]])
  (require ' [cpxlyze.parsers.git :refer [get-log]])

  (def path "../code-maat/")

  (clojure.string/split path #"/")

  (def log (get-log "../code-maat/"))
  (def loc (get-loc "../code-maat/"))


  (defn tree
    "yields a tree representation for d3.js"
    [data]
  (->> data
       (group-by #(-> % :path first))
       (map (fn [[k vs]]
              (let [m (assoc {} :name k)]
                (if (<= (count vs) 1)
                  (merge m (-> vs first (select-keys [:value])))
                  (update-in m [:children]
                             #(into []
                                    (flatten
                                     (conj %
                                           (tree
                                            (map (fn [x] (update x :path rest)) vs))))))))))))

  )
