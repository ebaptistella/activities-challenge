(ns challenge.handlers.http-server
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [io.pedestal.interceptor :as interceptor]))

(def health-check
  "Health check endpoint interceptor."
  (interceptor/interceptor
   {:name ::health-check
    :enter (fn [context]
             (assoc context
                    :response {:status 200
                               :headers {"Content-Type" "application/json"}
                               :body "{\"status\":\"ok\",\"service\":\"challenge\"}"}))}))

(def routes
  (route/expand-routes
   #{["/health"
      :get
      health-check
      :route-name :health-check]}))

(def server-config
  {::http/type :jetty
   ::http/routes routes})

(def server-config
  (merge server-config
         {::http/join? false}))