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
      :db/doc "lines of code"}])

(comment
  (require '[cpxlyze.parsers.git :refer [get-log]])

  (def cfg {:store {:backend :mem :id "example"} :initial-tx schema})

  (d/delete-database cfg)
  (d/create-database cfg)
  (def conn (d/connect cfg))

  (d/transact conn (into [] (get-log "../code-maat/")))

  (d/q '[:find ?name
         :where
         [_ :file/url ?name]]
       @conn)
  )
