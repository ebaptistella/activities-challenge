(ns challenge.infrastructure.http-server.activity
  (:require [challenge.adapters.activity :as adapters.activity]
            [challenge.controllers.activity :as controllers.activity]
            [challenge.interface.http.response :as response]
            [challenge.interceptors.components :as interceptors.components]))

(defn create-activity-handler
  [request]
  (try
    (let [persistency (interceptors.components/get-component request :persistency)
          activity-wire (:activity-wire request)
          activity-model (adapters.activity/wire->model activity-wire)
          result (controllers.activity/create-activity! activity-model persistency)
          response-wire (adapters.activity/model->wire result)]
      (response/created response-wire))
    (catch clojure.lang.ExceptionInfo e
      (response/bad-request (.getMessage e)))
    (catch Exception _
      (response/internal-server-error "Internal server error"))))

(defn get-activity-handler
  [request]
  (try
    (let [persistency (interceptors.components/get-component request :persistency)
          activity-id (Long/parseLong (get-in request [:path-params :id]))
          result (controllers.activity/get-activity activity-id persistency)]
      (if result
        (let [response-wire (adapters.activity/model->wire result)]
          (response/ok response-wire))
        (response/not-found "Activity not found")))
    (catch NumberFormatException _
      (response/bad-request "Invalid activity ID"))
    (catch Exception _
      (response/internal-server-error "Internal server error"))))

(defn list-activities-handler
  [request]
  (try
    (let [persistency (interceptors.components/get-component request :persistency)
          results (controllers.activity/list-activities persistency)
          response-wires (map adapters.activity/model->wire results)
          response-body {:activities response-wires}]
      (response/ok response-body))
    (catch Exception _
      (response/internal-server-error "Internal server error"))))

(defn update-activity-handler
  [request]
  (try
    (let [persistency (interceptors.components/get-component request :persistency)
          activity-id (Long/parseLong (get-in request [:path-params :id]))
          activity-wire (:activity-wire request)
          activity-model (adapters.activity/update-wire->model activity-wire)
          result (controllers.activity/update-activity! activity-id activity-model persistency)
          response-wire (adapters.activity/model->wire result)]
      (response/ok response-wire))
    (catch clojure.lang.ExceptionInfo e
      (if (= "Activity not found" (.getMessage e))
        (response/not-found (.getMessage e))
        (response/bad-request (.getMessage e))))
    (catch NumberFormatException _
      (response/bad-request "Invalid activity ID"))
    (catch Exception _
      (response/internal-server-error "Internal server error"))))

(defn delete-activity-handler
  [request]
  (try
    (let [persistency (interceptors.components/get-component request :persistency)
          activity-id (Long/parseLong (get-in request [:path-params :id]))
          _ (controllers.activity/delete-activity! activity-id persistency)]
      (response/no-content))
    (catch clojure.lang.ExceptionInfo e
      (if (= "Activity not found" (.getMessage e))
        (response/not-found (.getMessage e))
        (response/bad-request (.getMessage e))))
    (catch NumberFormatException _
      (response/bad-request "Invalid activity ID"))
    (catch Exception _
      (response/internal-server-error "Internal server error"))))
