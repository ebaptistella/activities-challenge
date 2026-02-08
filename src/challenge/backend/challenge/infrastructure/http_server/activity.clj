(ns challenge.infrastructure.http-server.activity
  (:require [challenge.adapters.activity :as adapters.activity]
            [challenge.components.logger :as logger]
            [challenge.controllers.activity :as controllers.activity]
            [challenge.interface.http.response :as response]))

(defn create-activity-handler
  [{:keys [activity-wire] componentes :componentes}]
  (let [{:keys [logger]} componentes
        log (logger/bound logger)]
    (logger/log-call log :debug
                     "[Handler] create-activity-handler called | activity-wire: %s | componentes keys: %s"
                     (pr-str activity-wire)
                     (keys componentes))
    (if (nil? activity-wire)
      (do
        (logger/log-call log :warn "[Handler] create-activity-handler: activity-wire is nil")
        (response/bad-request "Request body is required"))
      (try
        (let [{:keys [persistency]} componentes
              _ (logger/log-call log :debug
                                 "[Handler] create-activity-handler: persistency type: %s"
                                 (type persistency))
              activity-model (do
                               (logger/log-call log :debug
                                                "[Handler] create-activity-handler: calling wire->model")
                               (adapters.activity/wire->model activity-wire))
              _ (logger/log-call log :debug
                                 "[Handler] create-activity-handler: activity-model: %s"
                                 (pr-str activity-model))
              result (do
                       (logger/log-call log :debug
                                        "[Handler] create-activity-handler: calling create-activity!")
                       (controllers.activity/create-activity! activity-model persistency))
              _ (logger/log-call log :debug
                                 "[Handler] create-activity-handler: result: %s"
                                 (pr-str result))
              response-wire (adapters.activity/model->wire result)]
          (logger/log-call log :debug
                           "[Handler] create-activity-handler: success")
          (response/created response-wire))
        (catch Exception e
          (logger/log-call log :error
                           "[Handler] create-activity-handler: exception | Type: %s | Message: %s | Exception: %s"
                           (type e)
                           (.getMessage e)
                           (pr-str e))
          (throw e))))))

(defn get-activity-handler
  [{:keys [activity-id] componentes :componentes}]
  (let [{:keys [persistency]} componentes
        result (controllers.activity/get-activity activity-id persistency)]
    (if result
      (let [response-wire (adapters.activity/model->wire result)]
        (response/ok response-wire))
      (response/not-found "Activity not found"))))

(defn- query-params->filters
  "Extracts optional list filters from Pedestal query-params.
   Pedestal uses keyword keys for valid names (date, activity, activity_type).
   Query params: date (YYYY-MM-DD), activity (substring), activity_type (exact)."
  [query-params]
  (when query-params
    (let [date-v    (or (get query-params :date) (get query-params "date"))
          activity-v (or (get query-params :activity) (get query-params "activity"))
          type-v    (or (get query-params :activity_type) (get query-params "activity_type"))]
      (into {}
            (remove (fn [[_ v]] (or (nil? v) (empty? (str v)))))
            {:date date-v
             :activity activity-v
             :activity_type type-v}))))

(defn list-activities-handler
  "Receives the request map (same as other handlers); componentes and query-params come from request."
  [{:keys [query-params] componentes :componentes}]
  (let [{:keys [persistency]} componentes
        filters (query-params->filters query-params)
        results (controllers.activity/list-activities persistency filters)
        response-wires (map adapters.activity/model->wire results)
        response-body {:items response-wires}]
    (response/ok response-body)))

(defn update-activity-handler
  [{:keys [activity-wire activity-id] componentes :componentes}]
  (let [{:keys [logger]} componentes
        log (logger/bound logger)]
    (if (nil? activity-wire)
      (do
        (logger/log-call log :warn "[Handler] update-activity-handler: activity-wire is nil")
        (response/bad-request "Request body is required"))
      (let [{:keys [persistency]} componentes
            updates (adapters.activity/update-wire->model activity-wire)
            result (controllers.activity/update-activity! activity-id updates persistency)
            response-wire (adapters.activity/model->wire result)]
        (response/ok response-wire)))))

(defn delete-activity-handler
  [{:keys [activity-id] componentes :componentes}]
  (let [{:keys [persistency]} componentes
        _ (controllers.activity/delete-activity! activity-id persistency)]
    (response/no-content)))
