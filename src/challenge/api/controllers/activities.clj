(ns challenge.api.controllers.activities
  "Controllers for orchestrating activities operations."
  (:require [challenge.api.adapters :as adapters]
            [challenge.api.diplomat.database :as database]
            [challenge.api.logic :as logic]
            [challenge.api.models :as models]
            [clojure.data.json :as json]
            [clojure.tools.logging :as log]))

(defn query-activities!
  "Orchestrates the query for activities.
  
  Parameters:
  - ds: Database datasource
  - request: Ring request map with query parameters
  
  Returns:
  - Ring response map with activities or error"
  [ds request]
  (let [start-time (System/currentTimeMillis)
        query-params (:query-params request)
        filters (logic/extract-query-filters query-params)]
    (log/info "Starting query-activities!" {:filters filters})
    (if (nil? filters)
      (do
        (log/warn "Parameter 'date' not provided in request")
        {:status 400
         :headers {"Content-Type" "application/json"}
         :body (json/write-str (models/error-response "Parameter 'date' is required"))})
      (try
        (let [raw-activities (database/query-activities-raw ds {:date (:date filters)
                                                               :activity (:activity filters)
                                                               :activity_type (:activity_type filters)})
              _ (log/info "Raw activities received" {:count (count raw-activities)})
              enriched (logic/filter-activities-by-kind raw-activities (:type filters))
              result (models/activities-response enriched)
              response-time (- (System/currentTimeMillis) start-time)]
          (log/info "Query-activities! completed" {:filters filters
                                                   :raw-count (count raw-activities)
                                                   :enriched-count (count enriched)
                                                   :response-time-ms response-time})
          {:status 200
           :headers {"Content-Type" "application/json"}
           :body (json/write-str (adapters/model->wire-response result))})
        (catch Exception e
          (let [response-time (- (System/currentTimeMillis) start-time)]
            (log/error e "Error in query-activities!" {:filters filters :response-time-ms response-time})
            {:status 500
             :headers {"Content-Type" "application/json"}
             :body (json/write-str (models/error-response "Internal server error"))}))))))
