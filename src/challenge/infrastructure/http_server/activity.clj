(ns challenge.infrastructure.http-server.activity
  (:require [challenge.adapters.activity :as adapters.activity]
            [challenge.controllers.activity :as controllers.activity]
            [challenge.interface.http.response :as response]))

(defn create-activity-handler
  [{:keys [activity-wire] componentes :componentes}]
  (let [{:keys [persistency]} componentes
        activity-model (adapters.activity/wire->model activity-wire)
        result (controllers.activity/create-activity! activity-model persistency)
        response-wire (adapters.activity/model->wire result)]
    (response/created response-wire)))

(defn get-activity-handler
  [{:keys [path-params] componentes :componentes}]
  (let [{:keys [persistency]} componentes
        activity-id (Long/parseLong (get path-params :id))
        result (controllers.activity/get-activity activity-id persistency)]
    (if result
      (let [response-wire (adapters.activity/model->wire result)]
        (response/ok response-wire))
      (response/not-found "Activity not found"))))

(defn list-activities-handler
  [{componentes :componentes}]
  (let [{:keys [persistency]} componentes
        results (controllers.activity/list-activities persistency)
        response-wires (map adapters.activity/model->wire results)
        response-body {:activities response-wires}]
    (response/ok response-body)))

(defn update-activity-handler
  [{:keys [activity-wire path-params] componentes :componentes}]
  (let [{:keys [persistency]} componentes
        activity-id (Long/parseLong (get path-params :id))
        activity-model (adapters.activity/update-wire->model activity-wire)
        result (controllers.activity/update-activity! activity-id activity-model persistency)
        response-wire (adapters.activity/model->wire result)]
    (response/ok response-wire)))

(defn delete-activity-handler
  [{:keys [path-params] componentes :componentes}]
  (let [{:keys [persistency]} componentes
        activity-id (Long/parseLong (get path-params :id))
        _ (controllers.activity/delete-activity! activity-id persistency)]
    (response/no-content)))
