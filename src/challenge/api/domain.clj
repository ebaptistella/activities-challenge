(ns challenge.api.domain
  "Domain layer for orchestrating business operations."
  (:require [challenge.api.db :as db]
            [challenge.api.logic :as logic]
            [challenge.api.models :as models]
            [clojure.tools.logging :as log]))

(defn query-activities
  "Queries and enriches activities from database.
  
  Parameters:
  - ds: Database datasource
  - filters: Map with :date, :activity, :activity_type, :type
  
  Returns:
  - Vector of enriched activity maps"
  [ds {:keys [date activity activity_type type]}]
  (let [filters {:date date :activity activity :activity_type activity_type :type type}]
    (log/info "Starting query-activities" {:filters filters})
    (try
      (let [raw-activities (db/query-activities-raw ds {:date date
                                                        :activity activity
                                                        :activity_type activity_type})
            _ (log/info "Raw activities received" {:count (count raw-activities)})
            enriched (logic/filter-activities-by-kind raw-activities type)
            filtered-count (- (count raw-activities) (count enriched))]
        (log/info "Query-activities completed" {:filters filters
                                                :raw-count (count raw-activities)
                                                :enriched-count (count enriched)
                                                :filtered-out filtered-count})
        (when (pos? filtered-count)
          (log/warn "Some records were filtered because they don't have amount_planned or amount_executed"
                    {:filtered-count filtered-count
                     :raw-count (count raw-activities)
                     :type-filter type}))
        enriched)
      (catch Exception e
        (log/error e "Error executing query-activities" {:filters filters})
        (throw e)))))

(defn plano-x-realizado
  "Orchestrates the query for planned vs executed activities.
  
  Parameters:
  - ds: Database datasource
  - filters: Map with :date, :activity, :activity_type, :type
  
  Returns:
  - Map with :items vector containing enriched activities"
  [ds filters]
  (let [start-time (System/currentTimeMillis)]
    (log/info "Starting plano-x-realizado" {:filters filters})
    (try
      (let [activities (query-activities ds filters)
            result (models/activities-response activities)
            duration (- (System/currentTimeMillis) start-time)]
        (log/info "Plano-x-realizado completed" {:filters filters :items-count (count activities) :duration-ms duration})
        result)
      (catch Exception e
        (let [duration (- (System/currentTimeMillis) start-time)]
          (log/error e "Error executing plano-x-realizado" {:filters filters :duration-ms duration})
          (throw e))))))
