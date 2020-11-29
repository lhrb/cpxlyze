(ns cpxlyze.data.db
  (:require [datahike.api :as d]))

(def schema
    [;;commit
     {:db/ident :commit/rev
      :db/valueType :db.type/string
      :db/unique :db.unique/identity
      :db/cardinality :db.cardinality/one
      :db/doc "the commit revision hash"}
     {:db/ident :commit/author
      :db/valueType :db.type/ref
      :db/cardinality :db.cardinality/one
      :db/doc "commit author"}
     {:db/ident :commit/message
      :db/valueType :db.type/string
      :db/cardinality :db.cardinality/one
      :db/doc "commit message"}
     {:db/ident :commit/date
      :db/valueType :db.type/instant
      :db/cardinality :db.cardinality/one
      :db/doc "time of the commit"}
     {:db/ident :commit/changes
      :db/valueType :db.type/ref
      :db/cardinality :db.cardinality/many
      :db/doc "changes introduced by the commit"}

     ;; author
     {:db/ident :author/name
      :db/valueType :db.type/string
      :db/unique :db.unique/identity
      :db/cardinality :db.cardinality/one
      :db/doc "name of the author"}

     ;;changes
     {:db/ident :change/added
      :db/valueType :db.type/long
      :db/cardinality :db.cardinality/one
      :db/doc "loc added by the change"}
     {:db/ident :change/deleted
      :db/valueType :db.type/long
      :db/cardinality :db.cardinality/one
      :db/doc "loc deleted by the change"}
     {:db/ident :change/file
      :db/valueType :db.type/ref
      :db/cardinality :db.cardinality/one
      :db/doc "changed file"}

     ;;file
     {:db/ident :file/url
      :db/valueType :db.type/string
      :db/unique :db.unique/identity
      :db/cardinality :db.cardinality/one
      :db/doc "url of the file"}
     {:db/ident :file/name
      :db/valueType :db.type/string
      :db/cardinality :db.cardinality/one
      :db/doc "filename"}
     {:db/ident :file/loc
      :db/valueType :db.type/long
      :db/cardinality :db.cardinality/one
      :db/doc "lines of code"}
     {:db/ident :file/language
      :db/valueType :db.type/string
      :db/cardinality :db.cardinality/one}
     {:db/ident :file/comment
      :db/valueType :db.type/long
      :db/cardinality :db.cardinality/one}])

(comment
  (require '[cpxlyze.parsers.git :refer [get-log]])
  (require '[cpxlyze.parsers.cloc :refer [get-loc]])

  (def cfg {:store {:backend :mem :id "example"} :initial-tx schema})

  (d/delete-database cfg)
  (d/create-database cfg)
  (def conn (d/connect cfg))

  (get-loc "../code-maat/")

  (d/transact conn (into [] (get-log "../code-maat/")))
  (d/transact conn {:tx-data (into [] (get-loc "../code-maat/"))})

  (d/transact conn {:tx-data {:file/name "README.md"
                              :file/loc (int 0)}})

  (->> (into [] (get-loc "../code-maat/"))
       (map :file/comment)
       (map #(= (class %) java.lang.Long)))

  (d/q '[:find (pull ?e pattern)
         :in $ $date pattern
         :where
         [?e :commit/date ?date]
         [(> ?date $date)]]
       @conn da file-pattern)

  (->>
   (d/q '[:find ?c ?fname
          :in $ $date
          :where
          [?e :commit/date ?date]
          [(> ?date $date)]
          [?e :commit/changes ?c]
          [?c :change/file ?f]
          [?f :file/url ?fname]]
        @conn da)
   (map second))

  (second [1 2])

  (def file-pattern
    [{:commit/changes [{:change/file [:file/url]}]}])

  (->> result
       (reduce (fn [acc e] (conj acc (first e))) '()))

  (def da (date 2014 01 01))



;; => ([#:commit{:rev "25ed6cb"}]
;;     [#:commit{:rev "02a760a"}]
;;     [#:commit{:rev "61df934"}]
;;     [#:commit{:rev "d6524ab"}])

  '[~da]
  )

(defn to-date [^java.time.LocalDate date]
  (-> date
      (.atStartOfDay (java.time.ZoneId/systemDefault))
      (.toInstant)
      (java.util.Date/from)))

(defn date [year month day]
  (to-date (java.time.LocalDate/of year month day)))
