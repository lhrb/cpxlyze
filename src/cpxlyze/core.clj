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

(def path "../vega/")

(def log (get-log path "2019-01-01"))
(def loc (get-loc path))

(build-data loc log)

(oz/view! (get-cloud log)
          :mode :vega)

(oz/start-server!)

(oz/view! (get-viz-description (prepare-for-stratify (build-data loc log)))
          :mode
          :vega)

(oz/view! (-> (build-data loc log)
              (prepare-for-stratify)
              (add-main-author-name)
              (get-knowledge-dist))
          :mode
          :vega)


(->> (prepare-for-stratify log loc)
     (remove #(nil? (:revisions %)))
     (filter #(>= (:revisions %) 0.25)))

(oz/view! {:data {:name "test"
                  :values (->> (prepare-for-stratify log loc)
                              (remove #(nil? (:revisions %)))
                              (map #(select-keys % [:revisions])))}
           :mark "bar"
           :encoding {:x {:field "revisions" :bin {:binned true
                                              :step 0.05}}
                      :y {:aggregate "count"}}})




(def nodes (sum-of-coupling log))
(def links (link-data nodes (change-coupling-links log)))

(->> links
     (filter #(or (= (:source %) 1) (= (:target %) 1))))


(oz/view! (get-coupling-viz nodes links) :mode :vega)

(oz/view!
 [:div
  [:h1 "toller titel"]
  [:vega
     (get-coupling-viz nodes links)]
  ])
;; [:h1 "zweiter"]
;;  [:vega (get-viz-description (prepare-for-stratify (build-data loc log)))]


(sprint-interval "2020-11-11" "2019-01-01")

(let [data (sprint-summaries
                    (group-by-sprint
                     (sprint-interval "2020-11-11" "2019-01-01")
                     log))]
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
      :encoding {:y {:field "entities-changed" :type "quantitative"}
                 :x {:field "sprint" :type "temporal"}}}]]))


(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  
  (System/exit 0))
