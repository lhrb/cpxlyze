(ns cpxlyze.server
  (:require [ring.adapter.jetty :as jetty]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [reitit.ring.middleware.exception :as exception]
            [reitit.ring.middleware.multipart :as multipart]
            [reitit.ring.middleware.parameters :as parameters]
            [muuntaja.core :as m]
            [reitit.ring.coercion :as coercion]
            [reitit.ring :as ring]))

(defn logger [handler]
  (fn [request]
    (println (str (java.time.LocalDateTime/now) " Incoming Request:"))
    (clojure.pprint/pprint request)
    (let [response (handler request)]
      (println (str (java.time.LocalDateTime/now) " Outgoing Request:"))
      (clojure.pprint/pprint response)
      response)))

(def app
  (ring/ring-handler
   (ring/router
    ["/api"
     ["/ping" {:get {:handler (fn [req] {:status 200 :body (-> req :query-params :id)})}}]]
    {:data {:muuntaja m/instance
            :middleware [logger
                         ;; query-params & form-params
                         parameters/parameters-middleware
                         ;; content-negotiation
                         muuntaja/format-negotiate-middleware
                         ;; encoding response body
                         muuntaja/format-response-middleware
                         ;; exception handling
                         exception/exception-middleware
                         ;; decoding request body
                         muuntaja/format-request-middleware
                         ;; coercing response bodys
                         coercion/coerce-response-middleware
                         ;; coercing request parameters
                         coercion/coerce-request-middleware
                         ;; multipart
                         multipart/multipart-middleware]}})
   (ring/create-default-handler)))

(app {:request-method :get
      :uri "/api/ping"
      :query-params {:id 5 :x 5}})

(defonce server (jetty/run-jetty #'app {:port 7070 :join? false}))


(comment
  (require '[clojure.java.shell :as shell])
  (shell/sh "curl" "GET" "localhost:7070/api/ping?id=4")
  (shell/sh "curl" "-X" "PUT" "-d" "path=../vega/" "localhost:7070/api/repo")
  )
