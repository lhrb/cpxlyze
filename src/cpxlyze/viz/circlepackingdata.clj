(ns cpxlyze.viz.circlepackingdata)

(defn assoc-path [coll]
  (let [path (-> (:url coll)
                 (clojure.string/split #"/"))]
    (-> coll (assoc :path path))))

(defn- create-url [path]
  (clojure.string/join "/" path))

(defn- parent-path [path]
 (loop [rest-path path
        acc []]
   (let [[head & tail] rest-path]
    (if (nil? (first tail))
      (conj acc {:name head :parent nil :url head})
      (recur tail (conj acc {:name head
                             :parent (create-url (reverse tail))
                             :url (create-url (reverse rest-path))}))))))

(defn- create-hierachy [elem]
   (let [path (-> elem :path reverse)
         leaf (merge
               (select-keys elem [:url :revisions :code :authors])
               {:name (first path)
               :parent (create-url (reverse (rest path)))})]
     (if (nil? (:parent leaf))
       [leaf]
       (conj (parent-path (rest path)) leaf))))

(defn prepare-for-stratify [data]
  (->> data
       (map #(update % :url (fn [x] (str "root/" x))))
       (map assoc-path)
       (map create-hierachy)
       (flatten)
       (into [] (distinct))))

(defn add-main-author-name [data]
  (->> data
       (map (fn [e] (if (nil? (:authors e))
                      e
                      (assoc e :main-author-name
                             (->> (:authors e)
                                  (apply max-key :author/effort-abs)
                                  :author/name)))))))

(comment
  (require '[cpxlyze.parsers.cloc :refer [get-loc]])
  (require '[cpxlyze.parsers.git :refer [get-log]])
  (require '[cpxlyze.databuilder :refer [build-data]])

  (def path "../code-maat/")

  (def log (get-log path))
  (def loc (get-loc path))

  (-> (build-data loc log)
      (prepare-for-stratify)
      (add-main-author-name))

)
