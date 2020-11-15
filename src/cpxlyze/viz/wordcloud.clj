(ns cpxlyze.viz.wordcloud)

(defn- count-words [log]
  (->> log
      (map :message)
      (remove nil?)
      (reduce (fn [acc elem]
                (into acc
                      (clojure.string/split elem #" ")))
              [])
      (map clojure.string/upper-case)
      (frequencies)
      (reduce-kv (fn [m k v]
                   (conj m {:word k :count v}))
                 ())))

(defn- word-cloud [wordcount]
  {:width 500
   :height 500
   :data [{:name "table"
           :values wordcount}]
   :scales [{:name "color"
             :type "ordinal"
             :domain {:data "table" :field "word"}
             :range {:scheme "reds"}}]
   :marks [{:type "text"
            :from {:data "table"}
            :encode {:enter {:text {:field "word"}
                             :align {:value "center"}
                             :baseline {:value "alphabetic"}
                             :fill {:scale "color" :field "word"}}
                     :update {:fillOpacity {:value 1}}
                     :hover {:fillOpacity {:value 0.5}}}
            :transform [{:type "wordcloud"
                         :size [500, 500]
                         :text {:field "word"}
                         :fontSize {:field "datum.count"}
                         :fontWeight "bold"
                         :fontSizeRange [12, 56]
                         :padding 2}]}]})

(defn get-cloud [log]
  (-> log count-words word-cloud))
