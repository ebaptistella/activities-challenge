(ns challenge.infrastructure.http-server.activity
  (:require [challenge.adapters.activity :as adapters.activity]
            [challenge.controllers.activity :as controllers.activity]
            [challenge.interceptors.components :as interceptors.components]
            [challenge.wire.in.activity :as wire.in.activity]
            [cheshire.core :as json]
            [schema.core :as s]))

(defn- get-json-body
  [request]
  (or (:json-params request)
      (when-let [body (:body request)]
        (if (string? body)
          (json/parse-string body true)
          body))))

(defn- json-response
  [status body]
  (if (nil? body)
    {:status status
     :headers {"Content-Type" "application/json"}}
    {:status status
     :headers {"Content-Type" "application/json"}
     :body (json/generate-string body)}))

(defn create-activity-handler
  [request]
  (try
    (let [persistency (interceptors.components/get-component request :persistency)
          json-body (get-json-body request)
          activity-wire (s/validate wire.in.activity/ActivityRequest json-body)
          activity-model (adapters.activity/wire->model activity-wire)
          result (controllers.activity/create-activity! activity-model persistency)
          response-wire (adapters.activity/model->wire result)]
      (json-response 201 response-wire))
    (catch clojure.lang.ExceptionInfo e
      (json-response 400 {:error (.getMessage e)}))
    (catch Exception _
      (json-response 500 {:error "Internal server error"}))))

(defn get-activity-handler
  [request]
  (try
    (let [persistency (interceptors.components/get-component request :persistency)
          activity-id (Long/parseLong (get-in request [:path-params :id]))
          result (controllers.activity/get-activity activity-id persistency)]
      (if result
        (let [response-wire (adapters.activity/model->wire result)]
          (json-response 200 response-wire))
        (json-response 404 {:error "Activity not found"})))
    (catch NumberFormatException _
      (json-response 400 {:error "Invalid activity ID"}))
    (catch Exception _
      (json-response 500 {:error "Internal server error"}))))

(defn list-activities-handler
  [request]
  (try
    (let [persistency (interceptors.components/get-component request :persistency)
          results (controllers.activity/list-activities persistency)
          response-wires (map adapters.activity/model->wire results)
          response-body {:activities response-wires}]
      (json-response 200 response-body))
    (catch Exception _
      (json-response 500 {:error "Internal server error"}))))

(defn update-activity-handler
  [request]
  (try
    (let [persistency (interceptors.components/get-component request :persistency)
          activity-id (Long/parseLong (get-in request [:path-params :id]))
          json-body (get-json-body request)
          activity-wire (s/validate wire.in.activity/ActivityUpdateRequest json-body)
          activity-model (adapters.activity/update-wire->model activity-wire)
          result (controllers.activity/update-activity! activity-id activity-model persistency)
          response-wire (adapters.activity/model->wire result)]
      (json-response 200 response-wire))
    (catch clojure.lang.ExceptionInfo e
      (if (= "Activity not found" (.getMessage e))
        (json-response 404 {:error (.getMessage e)})
        (json-response 400 {:error (.getMessage e)})))
    (catch NumberFormatException _
      (json-response 400 {:error "Invalid activity ID"}))
    (catch Exception _
      (json-response 500 {:error "Internal server error"}))))

(defn delete-activity-handler
  [request]
  (try
    (let [persistency (interceptors.components/get-component request :persistency)
          activity-id (Long/parseLong (get-in request [:path-params :id]))
          _ (controllers.activity/delete-activity! activity-id persistency)]
      (json-response 204 nil))
    (catch clojure.lang.ExceptionInfo e
      (if (= "Activity not found" (.getMessage e))
        (json-response 404 {:error (.getMessage e)})
        (json-response 400 {:error (.getMessage e)})))
    (catch NumberFormatException _
      (json-response 400 {:error "Invalid activity ID"}))
    (catch Exception _
      (json-response 500 {:error "Internal server error"}))))
