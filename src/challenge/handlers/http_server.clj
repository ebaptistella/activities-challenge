(ns challenge.handlers.http-server
  (:require [challenge.infrastructure.http-server.activity :as http-server.activity]
            [challenge.infrastructure.http-server.health :as http-server.health]
            [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]))

(def routes
  (route/expand-routes
   #{["/health"
      :get
      http-server.health/health-check
      :route-name :health-check]

     ["/activities"
      :post
      http-server.activity/create-activity-handler
      :route-name :create-activity]

     ["/activities"
      :get
      http-server.activity/list-activities-handler
      :route-name :list-activities]

     ["/activities/:id"
      :get
      http-server.activity/get-activity-handler
      :route-name :get-activity]

     ["/activities/:id"
      :put
      http-server.activity/update-activity-handler
      :route-name :update-activity]

     ["/activities/:id"
      :delete
      http-server.activity/delete-activity-handler
      :route-name :delete-activity]}))

(def server-config
  (merge {::http/type :jetty
          ::http/routes routes}
         {::http/join? false}))