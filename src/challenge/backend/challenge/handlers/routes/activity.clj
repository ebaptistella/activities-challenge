(ns challenge.handlers.routes.activity
  (:require [challenge.infrastructure.http-server.activity :as http-server.activity]
            [challenge.interceptors.validation :as interceptors.validation]
            [challenge.wire.in.activity :as wire.in.activity]
            [challenge.wire.out.activity :as wire.out.activity]
            [challenge.wire.out.error :as wire.out.error]))

(def routes
  #{["/api/v1/activities"
     :post
     [interceptors.validation/json-body
      (interceptors.validation/validate-request-body wire.in.activity/ActivityRequest :activity-wire)
      http-server.activity/create-activity-handler]
     :route-name :create-activity
     :summary "Creates a new activity"
     :doc "Creates a new activity in the system"
     :request-body {:required true
                    :content {"application/json" {:schema wire.in.activity/ActivityRequest}}}
     :responses {201 {:body wire.out.activity/ActivityResponse
                      :description "Activity created successfully"}
                 400 {:body wire.out.error/ErrorResponse
                      :description "Invalid request"}
                 500 {:body wire.out.error/ErrorResponse
                      :description "Internal server error"}}]

    ["/api/v1/activities"
     :get
     http-server.activity/list-activities-handler
     :route-name :list-activities
     :summary "Lists all activities"
     :doc "Returns a list of all activities registered"
     :responses {200 {:body wire.out.activity/ListActivitiesResponse
                      :description "List of activities"}
                 500 {:body wire.out.error/ErrorResponse
                      :description "Internal server error"}}]

    ["/api/v1/activities/:id"
     :get
     [interceptors.validation/validate-path-params-id
      http-server.activity/get-activity-handler]
     :route-name :get-activity
     :summary "Returns an activity by ID"
     :doc "Searches for a specific activity by its ID"
     :responses {200 {:body wire.out.activity/ActivityResponse
                      :description "Activity found"}
                 400 {:body wire.out.error/ErrorResponse
                      :description "Invalid request"}
                 404 {:body wire.out.error/ErrorResponse
                      :description "Activity not found"}
                 500 {:body wire.out.error/ErrorResponse
                      :description "Internal server error"}}]

    ["/api/v1/activities/:id"
     :put
     [interceptors.validation/json-body
      (interceptors.validation/validate-request-body wire.in.activity/ActivityUpdateRequest :activity-wire)
      interceptors.validation/validate-path-params-id
      http-server.activity/update-activity-handler]
     :route-name :update-activity
     :summary "Updates an existing activity"
     :doc "Updates the data of an existing activity"
     :request-body {:required true
                    :content {"application/json" {:schema wire.in.activity/ActivityUpdateRequest}}}
     :responses {200 {:body wire.out.activity/ActivityResponse
                      :description "Activity updated successfully"}
                 400 {:body wire.out.error/ErrorResponse
                      :description "Invalid request"}
                 404 {:body wire.out.error/ErrorResponse
                      :description "Activity not found"}
                 500 {:body wire.out.error/ErrorResponse
                      :description "Internal server error"}}]

    ["/api/v1/activities/:id"
     :delete
     [interceptors.validation/validate-path-params-id
      http-server.activity/delete-activity-handler]
     :route-name :delete-activity
     :summary "Removes an activity"
     :doc "Removes an activity from the system"
     :responses {204 {:body nil
                      :description "Activity deleted successfully"}
                 400 {:body wire.out.error/ErrorResponse
                      :description "Invalid request"}
                 404 {:body wire.out.error/ErrorResponse
                      :description "Activity not found"}
                 500 {:body wire.out.error/ErrorResponse
                      :description "Internal server error"}}]})
