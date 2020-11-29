(ns cpxlyze.parsers.cloc
  (:require [clojure-csv.core :as csv]
            [clojure.string :refer [replace-first]]
            [cpxlyze.parsers.git :refer [exec-stream]]))

(defn cloc-cmd [path]
  (str "cloc " path " --by-file --csv --quiet"))

(defn read-cloc [cmd]
  (with-open [file (exec-stream cmd)]
    (doall (map (comp first csv/parse-csv) (line-seq file)))))

(defn drop-csv-metadata [coll]
  (-> coll rest butlast))

(defn- to-map [coll]
  (let [[lang name _ comment code] coll]
      {:file/language lang
       :file/url name
       :file/comment comment
       :file/loc code}))

(defn- remove-path-prefix [coll path]
  (let [name (replace-first (:file/url coll) path "")]
    (assoc coll :file/url name)))

(defn parse-cloc [path]
  (->> (read-cloc (cloc-cmd path))
       rest
       drop-csv-metadata
       (map to-map)
       (map #(remove-path-prefix % path))
       (map (fn [m] (-> m
                        (update :file/loc #(Long/valueOf %))
                        (update :file/comment #(Long/valueOf %)))))))

(defn kv-name-loc
  "transform to {\"filename\" 42}"
  [coll]
  (->> coll
       (reduce (fn [acc elem]
                 (assoc acc
                        (:file/url elem)
                        (:file/loc elem))) {})))

(defn get-loc
  "returns a map {filename loc}"
  [path]
  (-> path parse-cloc))

(comment

  (def path "../code-maat/")

  (-> path parse-cloc)

  )
