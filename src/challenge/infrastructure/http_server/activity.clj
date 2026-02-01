(ns challenge.infrastructure.http-server.activity
  (:require [challenge.adapters.activity :as adapters.activity]
            [challenge.controllers.activity :as controllers.activity]
            [challenge.interceptors.components :as interceptors.components]
            [challenge.wire.in.activity :as wire.in.activity]
            [cheshire.core :as json]
            [io.pedestal.interceptor :as interceptor]
            [schema.core :as s]))

(defn- get-json-body
  "Extracts JSON body from request and parses it.
   Checks :json-params first (set by json-body interceptor), then :body."
  [request]
  (or (:json-params request)
      (when-let [body (:body request)]
        (if (string? body)
          (json/parse-string body true)
          body))))

(defn- json-response
  "Creates a JSON response."
  [status body]
  {:status status
   :headers {"Content-Type" "application/json"}
   :body (json/generate-string body)})

(def create-activity-handler
  "Interceptor for creating a new activity.
   POST /activities"
  (interceptor/interceptor
   {:name ::create-activity
    :enter (fn [context]
             (try
               (let [request (:request context)
                     persistency (interceptors.components/get-component request :persistency)
                     json-body (get-json-body request)
                     activity-wire (s/validate wire.in.activity/ActivityRequest json-body)
                     activity-model (adapters.activity/wire->model activity-wire)
                     result (controllers.activity/create-activity! activity-model persistency)
                     response-wire (adapters.activity/model->wire result)]
                 (assoc context :response (json-response 201 response-wire)))
               (catch clojure.lang.ExceptionInfo e
                 (assoc context :response (json-response 400 {:error (.getMessage e)})))
               (catch Exception _
                 (assoc context :response (json-response 500 {:error "Internal server error"})))))}))

(def get-activity-handler
  "Interceptor for getting an activity by ID.
   GET /activities/:id"
  (interceptor/interceptor
   {:name ::get-activity
    :enter (fn [context]
             (try
               (let [request (:request context)
                     persistency (interceptors.components/get-component request :persistency)
                     activity-id (Long/parseLong (get-in request [:path-params :id]))
                     result (controllers.activity/get-activity! activity-id persistency)]
                 (if result
                   (let [response-wire (adapters.activity/model->wire result)]
                     (assoc context :response (json-response 200 response-wire)))
                   (assoc context :response (json-response 404 {:error "Activity not found"}))))
               (catch NumberFormatException _
                 (assoc context :response (json-response 400 {:error "Invalid activity ID"})))
               (catch Exception _
                 (assoc context :response (json-response 500 {:error "Internal server error"})))))}))

(def list-activities-handler
  "Interceptor for listing all activities.
   GET /activities"
  (interceptor/interceptor
   {:name ::list-activities
    :enter (fn [context]
             (try
               (let [request (:request context)
                     persistency (interceptors.components/get-component request :persistency)
                     results (controllers.activity/list-activities! persistency)
                     response-wires (map adapters.activity/model->wire results)
                     response-body {:activities response-wires}]
                 (assoc context :response (json-response 200 response-body)))
               (catch Exception _
                 (assoc context :response (json-response 500 {:error "Internal server error"})))))}))

(def update-activity-handler
  "Interceptor for updating an activity.
   PUT /activities/:id"
  (interceptor/interceptor
   {:name ::update-activity
    :enter (fn [context]
             (try
               (let [request (:request context)
                     persistency (interceptors.components/get-component request :persistency)
                     activity-id (Long/parseLong (get-in request [:path-params :id]))
                     json-body (get-json-body request)
                     activity-wire (s/validate wire.in.activity/ActivityUpdateRequest json-body)
                     activity-model (adapters.activity/update-wire->model activity-wire)
                     result (controllers.activity/update-activity! activity-id activity-model persistency)
                     response-wire (adapters.activity/model->wire result)]
                 (assoc context :response (json-response 200 response-wire)))
               (catch clojure.lang.ExceptionInfo e
                 (if (= "Activity not found" (.getMessage e))
                   (assoc context :response (json-response 404 {:error (.getMessage e)}))
                   (assoc context :response (json-response 400 {:error (.getMessage e)}))))
               (catch NumberFormatException _
                 (assoc context :response (json-response 400 {:error "Invalid activity ID"})))
               (catch Exception _
                 (assoc context :response (json-response 500 {:error "Internal server error"})))))}))

(def delete-activity-handler
  "Interceptor for deleting an activity.
   DELETE /activities/:id"
  (interceptor/interceptor
   {:name ::delete-activity
    :enter (fn [context]
             (try
               (let [request (:request context)
                     persistency (interceptors.components/get-component request :persistency)
                     activity-id (Long/parseLong (get-in request [:path-params :id]))
                     _ (controllers.activity/delete-activity! activity-id persistency)]
                 (assoc context :response (json-response 204 nil)))
               (catch clojure.lang.ExceptionInfo e
                 (if (= "Activity not found" (.getMessage e))
                   (assoc context :response (json-response 404 {:error (.getMessage e)}))
                   (assoc context :response (json-response 400 {:error (.getMessage e)}))))
               (catch NumberFormatException _
                 (assoc context :response (json-response 400 {:error "Invalid activity ID"})))
               (catch Exception _
                 (assoc context :response (json-response 500 {:error "Internal server error"})))))}))
