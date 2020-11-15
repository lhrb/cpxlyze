(ns cpxlyze.parsers.git
  (:require [instaparse.core :as insta]
            [clojure.java.shell :refer [sh]]))

;;; parser for output of
;;; git log --all -M -C --numstat --date=short --pretty=format:'--%h--%cd--%cn--%s'
(def log-file "resources/cb.log")

(def entry-grammar
  (insta/parser
   "
    entry = <prelude*> prelude changes
    <prelude> = <separator> rev <separator> date <separator> author <separator> message <nl>
    rev = #'[\\da-f]+'
    date = #'\\d{4}-\\d{2}-\\d{2}'
    author = #'[\\w{\\s\\w}]*'
    message = #'[^\\n]*'
    changes = change*
    change = added <tab> deleted <tab> filename <nl>
    added = #'\\d+'
    deleted = #'\\d+'
    filename = #'[^\\n]*'
    separator = '--'
    tab = #'\\t*'
    nl = #'\\n*'
   "))

(defn to-map
  [entry]
    {:rev (get-in entry [1 1])
     :date (get-in entry [2 1])
     :author (get-in entry [3 1])
     :message (get-in entry [4 1])
     :changes (->> (rest (get-in entry [5]))
                   (map #(into {} (rest %))))})

(defn update-last
  "updates the last element in a vector m with the function f"
  [m f]
  (if (empty? m)
    (conj [] (vec (f nil)))
    (conj (pop m) (f (peek m)))))

(defn- split2
  [acc elem pred]
  (if (pred elem)
    (conj acc [])
    (update-last acc #(conj % elem))))

(defn split-when
  "returns a function which either starts a new collection when
  the given predicate is fulfilled or appends to the last collection.
  "
  [pred]
  (fn [acc elem] (split2 acc elem pred)))

(defn git-log
  ([path after-date]
   (->>
    (sh "git"
        "-C" path
        "log"
        "--all"
        "-C"
        "-M"
        (str "--after="after-date)
        "--pretty=format:--%h--%cd--%cn--%s"
        "--date=short"
        "--numstat")
    :out
    (spit log-file))))

(defn read-log [filename]
  (with-open [rdr (java.io.BufferedReader. (java.io.FileReader. filename))]
    (->> (line-seq rdr)
         (reduce (split-when empty?) [[]])
         doall)))

(defn parse-log [filename]
  (->> (read-log filename)
       (map #(->> % (map (fn [x] (str x \newline))) (apply str)))
       (map entry-grammar)
       (map to-map)))

(defn get-log
  ([path]
   (get-log path "1970-01-01"))
  ([path after-date]
   (do
     (git-log path after-date)
     (parse-log log-file))))

(comment

  (def entry-str
    "--5f84c6b--2020-01-25--Author name--Commit message
3\t3\tREADME.md")

  (entry-grammar entry-str)

  (to-map
   [:entry
    [:rev "ebd2b76"]
    [:date "2020-01-25"]
    [:author "Name with Spaces"]
    [:message "Add a commit message for tests"]
    [:changes [:change [:added "3"] [:deleted "3"] [:filename "README.md"]]]])

  ;; experimental
  (let [rdr (java.io.BufferedReader. (java.io.StringReader. "1\n2\n\n3\n\n"))]
    (loop [line (.readLine rdr)
           acc []]
      (if (nil? line)
        acc
        (recur (.readLine rdr) (conj acc (tokenize rdr line))))))

  (defn tokenize [^java.io.BufferedReader rdr ^String current-line]
    (loop [line current-line
           result ""]
      (if (empty? line)
        result
        (recur (.readLine rdr) (str result line \newline))))))
