(ns cpxlyze.sprintdata
  (:require [cpxlyze.inspect :refer [summary]]))

(defn- interval [from-date until-date]
  (let [formatter (java.time.format.DateTimeFormatter/ofPattern "yyyy-MM-dd")]
   (loop [acc []
          from (java.time.LocalDate/parse from-date formatter)
          until (java.time.LocalDate/parse until-date formatter)]
     (if (.isBefore from until)
       acc
       (recur (conj acc from) (.minusDays from 14) until)))))

(defn sprint-interval [from-date until-date]
 (loop [acc []
        dates (sort (interval from-date until-date))]
   (let [start (first dates)
         end (second dates)]
     (if (nil? end)
       acc
       (recur (conj acc {:sprint/start start
                         :sprint/end (.minusDays end 1)})
              (rest dates))))))

(defn inside-interval? [start-date end-date date-to-test]
  (and (.isAfter date-to-test start-date)
       (.isBefore date-to-test end-date)))

(defn find-index [pred xs]
  (reduce (fn [_ [i item]]
            (when (pred item) (reduced i)))
          (map vector (range) xs)))

(defn group-by-sprint [sprints log]
  (let [formatter (java.time.format.DateTimeFormatter/ofPattern "yyyy-MM-dd")]
   (->> log
        (remove #(nil? (:date %)))
        (reduce
         (fn [acc elem]
           (let [idx (find-index #(inside-interval?
                                   (:sprint/start %)
                                   (:sprint/end %)
                                   (-> elem
                                       :date
                                       (java.time.LocalDate/parse formatter)))
                                 acc)]
             (if (nil? idx)
               acc
               (update-in acc [idx :data] #(conj % elem)))))
         sprints))))

(defn sprint-summaries [sprints]
  (let [formatter (java.time.format.DateTimeFormatter/ofPattern "yyyy-MM-dd")]
   (->> sprints
        (map (fn [m] (assoc (summary (:data m))
                            :sprint (.format (:sprint/start m) formatter)))))))

(comment
  (require '[cpxlyze.parsers.git :refer [get-log]])
  (def log (get-log "../vega/" "2019-01-01"))


  (->> (group-by-sprint (sprint-interval "2020-11-11" "2019-01-01") log)
       (sprint-summaries))
  )
