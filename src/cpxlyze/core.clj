(ns cpxlyze.core
  (:require
   [oz.core :as oz]
   [cpxlyze.parsers.git :refer [get-log]]
   [cpxlyze.parsers.cloc :refer [get-loc]]
   [cpxlyze.viz.circlepackingdata :refer [prepare-for-stratify
                                          add-main-author-name]]
   [cpxlyze.viz.hotspots :refer [get-viz-description]]
   [cpxlyze.viz.wordcloud :refer [get-cloud]]
   [cpxlyze.viz.knowledgedist :refer [get-knowledge-dist]]
   [cpxlyze.databuilder :refer [build-data]]
   [cpxlyze.coupling :refer [change-coupling-links
                             sum-of-coupling
                             link-data]]
   [cpxlyze.viz.coupling :refer [get-coupling-viz]]
   [cpxlyze.sprintdata :refer [sprint-interval
                               group-by-sprint
                               sprint-summaries]]
   [cheshire.core :as json])
  (:gen-class))

(defn word-cloud [log]
  (oz/view! (get-cloud log)
           :mode :vega))

(defn hotspots-viz [stratified]
 (oz/view! (get-viz-description stratified)
           :mode
           :vega))

(defn knowledge-viz [stratified]
 (oz/view! (-> stratified
               (add-main-author-name)
               (get-knowledge-dist))
           :mode
           :vega))

(defn coupling-viz [log]
  (let [nodes (sum-of-coupling log)
        links (link-data nodes (change-coupling-links log))]
    (oz/view! (get-coupling-viz nodes links)
              :mode
              :vega)))

(defn sprint-summary-viz [data]
  (oz/view!
   [:div
    [:vega-lite
     {:width 800
      :data {:values data}
      :mark "line"
      :encoding {:y {:field "commits" :type "quantitative"}
                 :x {:field "sprint" :type "temporal"}}}]
    [:vega-lite
     {:width 800
      :data {:values data}
      :mark "line"
      :encoding {:y {:field "added" :type "quantitative"}
                 :x {:field "sprint" :type "temporal"}}}]
    [:vega-lite
     {:width 800
      :data {:values data}
      :mark "line"
      :encoding {:y {:field "deleted" :type "quantitative"}
                 :x {:field "sprint" :type "temporal"}}}]
    [:vega-lite
     {:width 800
      :data {:values data}
      :mark "line"
      :encoding {:y {:field "entities" :type "quantitative"}
                 :x {:field "sprint" :type "temporal"}}}]]))


(comment
  (def path "../vega/")

  (def log (get-log path "2019-01-01"))
  (def loc (get-loc path))

  (oz/start-server!)

  (let [stratified (prepare-for-stratify (build-data loc log))]
                                        ;(hotspots-viz stratified)
                                        ;(knowledge-viz stratified)
    )

  (sprint-summary-viz (->> log
                           (group-by-sprint (sprint-interval "2020-11-11" "2019-01-01"))
                           (sprint-summaries)))

  (let [last-sprint (->> log
                         (group-by-sprint
                          (sprint-interval "2020-11-11" "2019-01-01"))
                         last
                         :data)
        stratified (prepare-for-stratify (build-data loc last-sprint))]
    (hotspots-viz stratified)
    ;(knowledge-viz stratified)
    ;(word-cloud last-sprint)
    ;(coupling-viz last-sprint)
    )

 )


(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  
  (System/exit 0))
