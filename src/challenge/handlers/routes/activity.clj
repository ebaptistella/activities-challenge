(ns challenge.handlers.routes.activity
  (:require [challenge.infrastructure.http-server.activity :as http-server.activity]
            [challenge.interceptors.validation :as interceptors.validation]
            [challenge.wire.in.activity :as wire.in.activity]))

(def routes
  #{["/activities"
     :post
     [interceptors.validation/json-body
      (interceptors.validation/validate-request-body wire.in.activity/ActivityRequest :activity-wire)
      http-server.activity/create-activity-handler]
     :route-name :create-activity]

    ["/activities"
     :get
     http-server.activity/list-activities-handler
     :route-name :list-activities]

    ["/activities/:id"
     :get
     [interceptors.validation/validate-path-params-id
      http-server.activity/get-activity-handler]
     :route-name :get-activity]

    ["/activities/:id"
     :put
     [interceptors.validation/json-body
      (interceptors.validation/validate-request-body wire.in.activity/ActivityUpdateRequest :activity-wire)
      interceptors.validation/validate-path-params-id
      http-server.activity/update-activity-handler]
     :route-name :update-activity]

    ["/activities/:id"
     :delete
     [interceptors.validation/validate-path-params-id
      http-server.activity/delete-activity-handler]
     :route-name :delete-activity]})
