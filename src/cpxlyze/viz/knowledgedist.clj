(ns cpxlyze.viz.knowledgedist)

(defn- data [values]
  [{:name "tree"
    :values values
    :transform [{:type "stratify"
                 :key "url"
                 :parentKey "parent"}
                {:type "pack"
                 :field "code"
                 :sort {:field "code"}
                 :size [{:signal "width"} {:signal "height"}]}]}
   {:name "links"
    :source "tree"
    :transform [{:type "treelinks"}]}])

(defn- scales []
  [{:name "author"
    :type "ordinal"
    :domain {:data "tree"
             :field "main-author-name"}
    :range {:scheme "category20"}}
   {:name "depth-color"
    :type "linear"
    :domain {:data "tree"
             :field "depth"}
    :range {:scheme "purples"}}])

(defn- mark-tree-depth []
  {:type "symbol"
   :from {:data "tree"}
   :encode
   {:enter
    {:shape {:value "circle"}
     :fill {:scale "depth-color" :field "depth"}
     :tooltip
     {:signal
      "datum.name + (datum.size ? ', ' + datum.size + ' bytes' : '')"}},
    :update
    {:x {:signal "datum.x * scaleX + xTranslation"}
     :y {:signal "datum.y * scaleY + yTranslation"}
     :size {:signal "4 * datum.r * datum.r * scaleX * scaleY"}
     :stroke {:value "white"}
     :strokeWidth {:value 0.5}}
    :hover {:stroke {:value "black"}, :strokeWidth {:value 2}}}})

(defn- mark-authors []
  {:type "symbol",
   :from {:data "tree"},
   :encode
   {:enter
    {:shape {:value "circle"},
     :fill {:scale "author", :field "main-author-name"},
     :opacity [{:test "isDefined(datum['main-author-name'])" :value 1}
               {:value 0}]
     :tooltip
     {:signal
      "datum.name + (datum.size ? ', ' + datum.size + ' bytes' : '')"}},
    :update
    {:x {:signal "datum.x * scaleX + xTranslation"},
     :y {:signal "datum.y * scaleY + yTranslation"},
     :size {:signal "4 * datum.r * datum.r * scaleX * scaleY"}
     :strokeWidth {:value 0}}
    :hover {:stroke {:value "red"}, :strokeWidth {:value 2}}}})

(defn- signals []
  [{:name "scaleX"
    :value 1
    :on
    [{:events "click"
      :update "(width / 2) / datum.r"}]}
   {:name "scaleY"
    :value 1
    :on
    [{:events "click"
      :update "(height / 2) / datum.r"}]}
   {:name "xTranslation"
    :value 0
    :on [{:events "click" :update "(width / 2) - datum.x * scaleX"}]}
   {:name "yTranslation"
    :value 0
    :on [{:events "click" :update "(height / 2) - datum.y * scaleY"}]}
   ])

(defn- legends []
  [{:stroke "author"
    :fill "author"
    :titel "Main author"
    :encode {:symbols
             {:enter {:size {:value 350}} }}}])

(defn get-knowledge-dist
  [data-vals]
  {:title {:text "Knowledge distribution"}
   :width 932
   :height 932
   :data (data data-vals)
   :scales (scales)
   :signals (signals)
   :marks [(mark-tree-depth)
           (mark-authors)]
   :legends (legends)}
  )
