(ns volis-challenge.domain
  (:require
   [volis-challenge.db :as db]
   [clojure.tools.logging :as log]))

(defn calculate-kind
  [amount-planned amount-executed]
  (cond
    (and (some? amount-planned) (some? amount-executed)) "both"
    (some? amount-planned) "planned"
    (some? amount-executed) "executed"
    :else nil))

(defn select-relevant-amount
  [amount-planned amount-executed type]
  (case type
    "planned" amount-planned
    "executed" amount-executed
    (or amount-executed amount-planned)))

(defn enrich-activity
  [activity-row type-filter]
  (let [activity (:activity activity-row)
        activity_type (:activity_type activity-row)
        unit (:unit activity-row)
        amount_planned (:amount_planned activity-row)
        amount_executed (:amount_executed activity-row)
        kind (calculate-kind amount_planned amount_executed)
        amount (select-relevant-amount amount_planned amount_executed type-filter)]
    (log/debug "Enriching activity" {:activity-row activity-row
                                         :activity activity
                                         :amount_planned amount_planned
                                         :amount_executed amount_executed
                                         :amount_planned-type (type amount_planned)
                                         :amount_executed-type (type amount_executed)
                                         :kind kind
                                         :type-filter type-filter})
    (if kind
      {:activity activity
       :activity_type activity_type
       :unit unit
       :amount amount
       :kind kind}
      (do
        (log/warn "Record without kind (will be filtered)" {:activity activity
                                                       :amount_planned amount_planned
                                                       :amount_executed amount_executed})
        nil))))

(defn query-activities
  [ds {:keys [date activity activity_type type]}]
  (let [filters {:date date :activity activity :activity_type activity_type :type type}]
    (log/info "Starting query-activities" {:filters filters})
    (try
      (let [raw-activities (db/query-activities-raw ds {:date date
                                                        :activity activity
                                                        :activity_type activity_type})
            _ (log/info "Raw activities received" {:count (count raw-activities)
                                                    :first-row (when (seq raw-activities) (first raw-activities))
                                                    :first-row-keys (when (seq raw-activities) (keys (first raw-activities)))})
            enriched (->> raw-activities
                          (map-indexed (fn [idx row]
                                         (log/debug "Processing record" {:index idx
                                                                            :row row
                                                                            :keys (keys row)
                                                                            :amount_planned (:amount_planned row)
                                                                            :amount_executed (:amount_executed row)})
                                         (let [result (enrich-activity row type)]
                                           (when (nil? result)
                                             (log/warn "Record filtered (kind nil)" {:index idx
                                                                                       :row row
                                                                                       :amount_planned (:amount_planned row)
                                                                                       :amount_executed (:amount_executed row)
                                                                                       :amount_planned-type (class (:amount_planned row))
                                                                                       :amount_executed-type (class (:amount_executed row))}))
                                           result)))
                          (remove nil?))
            filtered-count (- (count raw-activities) (count enriched))]
        (log/info "Query-activities completed" {:filters filters
                                                :raw-count (count raw-activities)
                                                :enriched-count (count enriched)
                                                :filtered-out filtered-count
                                                :sample-enriched (when (seq enriched) (first enriched))})
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
  [ds filters]
  (let [start-time (System/currentTimeMillis)]
    (log/info "Starting plano-x-realizado" {:filters filters})
    (try
      (let [activities (query-activities ds filters)
            result {:items activities}
            duration (- (System/currentTimeMillis) start-time)]
        (log/info "Plano-x-realizado completed" {:filters filters :items-count (count activities) :duration-ms duration})
        result)
      (catch Exception e
        (let [duration (- (System/currentTimeMillis) start-time)]
          (log/error e "Error executing plano-x-realizado" {:filters filters :duration-ms duration})
          (throw e))))))
