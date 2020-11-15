(ns cpxlyze.parsers.cloc
  (:require [clojure.java.shell :refer [sh]]
            [clojure-csv.core :as csv]
            [clojure.string :refer [replace-first]]))

(def csv-file "resources/loc.csv")

(defn cloc [path]
  (sh "cloc" path "--by-file" "--csv" "--quiet" (str "--out=" csv-file)))

(defn read-cloc [filename]
  (with-open [file (java.io.BufferedReader. (java.io.FileReader. filename))]
    (csv/parse-csv (slurp file))))

(defn drop-csv-metadata [coll]
  (-> coll rest butlast))

(defn- to-map [coll]
  (let [[lang name _ comment code] coll]
      {:language lang
       :filename name
       :comment comment
       :code code}))

(defn- remove-path-prefix [coll path]
  (let [name (replace-first (:filename coll) path "")]
    (assoc coll :filename name)))

(defn parse-cloc [path]
  (->> (read-cloc csv-file)
       drop-csv-metadata
       (map to-map)
       (map #(remove-path-prefix % path))))

(defn kv-name-loc
  "transform to {\"filename\" 42}"
  [coll]
  (->> coll
       (reduce (fn [acc elem]
                 (assoc acc
                        (:filename elem)
                        (Integer/valueOf(:code elem)))) {})))

(defn get-loc
  "returns a map {filename loc}"
  [path]
  (do
    (cloc path)
    (-> path parse-cloc kv-name-loc)))

(comment

  (def path "../code-maat/")

  ;; generate file
  (cloc path)

  (-> path parse-cloc)

  )
