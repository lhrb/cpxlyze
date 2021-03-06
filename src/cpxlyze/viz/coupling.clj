(ns cpxlyze.viz.coupling)

(defn data [nodes links]
  [{:name "node-data"
    :values nodes}
   {:name "link-data"
    :values links}
   {:name "selected"
    :values links
    :transform [{:type "filter"
                 :expr "datum.source === active || datum.target === active"}]}])

(defn scales []
  [{:name "color"
    :type "linear"
    :domain {:data "node-data" :field "value"}
    :range {:scheme "blues"}}])

(defn marks []
  [{:name "nodes"
    :type "symbol"
    :zindex 1
    :from {:data "node-data"}
    :on
    [{:trigger "fix"
      :modify "node"
      :values
      "fix === true ? {fx: node.x, fy: node.y} : {fx: fix[0], fy: fix[1]}"}
     {:trigger "!fix", :modify "node", :values "{fx: null, fy: null}"}]
    :encode
    {:enter {:fill {:scale "color", :field "value"},
             :stroke {:value "white"}
             :strokeWidth {:value 3}
             :tooltip
             {:signal
              "datum.name"}}
     :update
     {:size {:signal "8 * datum.value"}
      :cursor {:value "pointer"}
      :stroke [{:test "active === datum.index", :value "green"}
               {:test "indata('selected', 'source', datum.index)", :value "yellow"}
               {:test "indata('selected', 'target', datum.index)", :value "red"}
               {:value "white"}]}}
    :transform
    [{:type "force"
      :iterations 300
      :restart {:signal "restart"}
      :static {:signal "static"}
      :signal "force"
      :forces
      [{:force "center", :x {:signal "cx"}, :y {:signal "cy"}}
       {:force "collide", :radius {:signal "nodeRadius"}}
       {:force "nbody", :strength {:signal "nodeCharge"}}
       {:force "link",
        :links "link-data",
        :id {:field "index"}
        :distance {:signal "linkDistance"}}]}]}
   {:type "path",
    :from {:data "link-data"},
    :interactive false,
    :encode {:update {:stroke {:value "#ccc"}, :strokeWidth {:value 0.5}}},
    :transform
    [{:type "linkpath",
      :require {:signal "force"},
      :shape "line",
      :sourceX "datum.source.x",
      :sourceY "datum.source.y",
      :targetX "datum.target.x",
      :targetY "datum.target.y"}]}])

(defn signals []
  [{:name "cx", :update "width / 2"}
   {:name "cy", :update "height / 2"}
   {:name "nodeRadius",
    :value 20}
   {:name "nodeCharge",
    :value -50,}
   {:name "linkDistance",
    :value 100}
   {:name "static", :value true}
   {:description "State variable for active node fix status.",
    :name "fix",
    :value false,
    :on
    [{:events "symbol:mouseout[!event.buttons], window:mouseup",
      :update "false"}
     {:events "symbol:mouseover", :update "fix || true"}
     {:events "[symbol:mousedown, window:mouseup] > window:mousemove!",
      :update "xy()",
      :force true}]}
   {:description "Graph node most recently interacted with.",
    :name "node",
    :value nil,
    :on [{:events "symbol:mouseover", :update "fix === true ? item() : node"}]}
   {:description "Flag to restart Force simulation upon data changes.",
    :name "restart",
    :value false,
    :on [{:events {:signal "fix"}, :update "fix && fix.length"}]}
   {:name "active"
    :value nil
    :on [{:events "click" :update "datum.index"}]}])

(defn get-coupling-viz [nodes links]
  {:$schema "https://vega.github.io/schema/vega/v5.json"
      :description
      "A node-link diagram with force-directed layout, depicting character co-occurrence in the novel Les Misérables.",
      :autosize "none"
      :width 1300
      :height 900
      :padding 0
      :data (data nodes links)
      :scales (scales)
      :marks (marks)
      :signals (signals)
      })
