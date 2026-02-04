(ns challenge.infrastructure.http-server.swagger
  (:require [challenge.interface.http.response :as response]
            [clojure.java.io :as io]))

(defn- generate-openapi-spec
  []
  {:openapi "3.0.0"
   :info {:title "Challenge API"
          :version "1.0.0"
          :description "API for managing activities"}
   :paths {"/health"
           {:get {:summary "Health check endpoint"
                  :description "Checks if the service is running"
                  :responses {"200" {:description "Service is healthy"
                                     :content {"application/json" {:schema {:type "object"
                                                                            :properties {:status {:type "string"}
                                                                                         :service {:type "string"}}}}}}}}}
           "/activities"
           {:get {:summary "Lists all activities"
                  :description "Returns a list of all activities registered"
                  :responses {"200" {:description "List of activities"
                                     :content {"application/json" {:schema {:type "object"
                                                                            :properties {:activities {:type "array"
                                                                                                      :items {:type "object"
                                                                                                              :properties {:id {:type "integer"}
                                                                                                                           :date {:type "string"}
                                                                                                                           :activity {:type "string"}
                                                                                                                           :activity-type {:type "string"}
                                                                                                                           :unit {:type "string"}
                                                                                                                           :amount-planned {:type "number"}
                                                                                                                           :amount-executed {:type "number"}
                                                                                                                           :created-at {:type "string"}
                                                                                                                           :updated-at {:type "string"}}}}}}}}}
                              "500" {:description "Internal server error"
                                     :content {"application/json" {:schema {:type "object"
                                                                            :properties {:error {:type "string"}}}}}}}
                  :post {:summary "Creates a new activity"
                         :description "Creates a new activity in the system"
                         :requestBody {:required true
                                       :content {"application/json" {:schema {:type "object"
                                                                              :required [:date :activity :activity-type :unit]
                                                                              :properties {:date {:type "string"}
                                                                                           :activity {:type "string"}
                                                                                           :activity-type {:type "string"}
                                                                                           :unit {:type "string"}
                                                                                           :amount-planned {:type "number"}
                                                                                           :amount-executed {:type "number"}}}}}}
                         :responses {"201" {:description "Activity created successfully"
                                            :content {"application/json" {:schema {:type "object"
                                                                                   :properties {:id {:type "integer"}
                                                                                                :date {:type "string"}
                                                                                                :activity {:type "string"}
                                                                                                :activity-type {:type "string"}
                                                                                                :unit {:type "string"}
                                                                                                :amount-planned {:type "number"}
                                                                                                :amount-executed {:type "number"}
                                                                                                :created-at {:type "string"}
                                                                                                :updated-at {:type "string"}}}}}}
                                     "400" {:description "Invalid request"
                                            :content {"application/json" {:schema {:type "object"
                                                                                   :properties {:error {:type "string"}}}}}}
                                     "500" {:description "Internal server error"
                                            :content {"application/json" {:schema {:type "object"
                                                                                   :properties {:error {:type "string"}}}}}}}}}
            "/activities/{id}"
            {:get {:summary "Returns an activity by ID"
                   :description "Searches for a specific activity by its ID"
                   :parameters [{:name "id"
                                 :in "path"
                                 :required true
                                 :schema {:type "integer"}}]
                   :responses {"200" {:description "Activity found"
                                      :content {"application/json" {:schema {:type "object"
                                                                             :properties {:id {:type "integer"}
                                                                                          :date {:type "string"}
                                                                                          :activity {:type "string"}
                                                                                          :activity-type {:type "string"}
                                                                                          :unit {:type "string"}
                                                                                          :amount-planned {:type "number"}
                                                                                          :amount-executed {:type "number"}
                                                                                          :created-at {:type "string"}
                                                                                          :updated-at {:type "string"}}}}}}
                               "400" {:description "Invalid request"
                                      :content {"application/json" {:schema {:type "object"
                                                                             :properties {:error {:type "string"}}}}}}
                               "404" {:description "Activity not found"
                                      :content {"application/json" {:schema {:type "object"
                                                                             :properties {:error {:type "string"}}}}}}
                               "500" {:description "Internal server error"
                                      :content {"application/json" {:schema {:type "object"
                                                                             :properties {:error {:type "string"}}}}}}}}
             :put {:summary "Updates an existing activity"
                   :description "Updates the data of an existing activity"
                   :parameters [{:name "id"
                                 :in "path"
                                 :required true
                                 :schema {:type "integer"}}]
                   :requestBody {:required true
                                 :content {"application/json" {:schema {:type "object"
                                                                        :properties {:date {:type "string"}
                                                                                     :activity {:type "string"}
                                                                                     :activity-type {:type "string"}
                                                                                     :unit {:type "string"}
                                                                                     :amount-planned {:type "number"}
                                                                                     :amount-executed {:type "number"}}}}}}
                   :responses {"200" {:description "Activity updated successfully"
                                      :content {"application/json" {:schema {:type "object"
                                                                             :properties {:id {:type "integer"}
                                                                                          :date {:type "string"}
                                                                                          :activity {:type "string"}
                                                                                          :activity-type {:type "string"}
                                                                                          :unit {:type "string"}
                                                                                          :amount-planned {:type "number"}
                                                                                          :amount-executed {:type "number"}
                                                                                          :created-at {:type "string"}
                                                                                          :updated-at {:type "string"}}}}}}
                               "400" {:description "Invalid request"
                                      :content {"application/json" {:schema {:type "object"
                                                                             :properties {:error {:type "string"}}}}}}
                               "404" {:description "Activity not found"
                                      :content {"application/json" {:schema {:type "object"
                                                                             :properties {:error {:type "string"}}}}}}
                               "500" {:description "Internal server error"
                                      :content {"application/json" {:schema {:type "object"
                                                                             :properties {:error {:type "string"}}}}}}}}
             :delete {:summary "Removes an activity"
                      :description "Removes an activity from the system"
                      :parameters [{:name "id"
                                    :in "path"
                                    :required true
                                    :schema {:type "integer"}}]
                      :responses {"204" {:description "Activity deleted successfully"}
                                  "400" {:description "Invalid request"
                                         :content {"application/json" {:schema {:type "object"
                                                                                :properties {:error {:type "string"}}}}}}
                                  "404" {:description "Activity not found"
                                         :content {"application/json" {:schema {:type "object"
                                                                                :properties {:error {:type "string"}}}}}}
                                  "500" {:description "Internal server error"
                                         :content {"application/json" {:schema {:type "object"
                                                                                :properties {:error {:type "string"}}}}}}}}}}}})

(defn swagger-json-handler
  [_request]
  (let [spec (generate-openapi-spec)]
    (response/ok spec)))

(defn swagger-ui-handler
  [_request]
  (let [swagger-ui-file (io/resource "public/swagger-ui.html")
        swagger-ui-html (if swagger-ui-file
                          (slurp swagger-ui-file)
                          (throw (ex-info "swagger-ui.html not found in resources/public/"
                                          {:resource-path "public/swagger-ui.html"})))]
    {:status 200
     :headers {"Content-Type" "text/html"
               "Content-Security-Policy" "default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval' https://unpkg.com; style-src 'self' 'unsafe-inline' https://unpkg.com; font-src 'self' https://unpkg.com; img-src 'self' data: https:; connect-src 'self';"}
     :body swagger-ui-html}))
