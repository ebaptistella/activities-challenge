(ns volis-challenge.domain
  (:require
   [volis-challenge.db :as db]))

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
  (let [{:keys [activity activity_type unit amount_planned amount_executed]} activity-row
        kind (calculate-kind amount_planned amount_executed)
        amount (select-relevant-amount amount_planned amount_executed type-filter)]
    (when kind
      {:activity activity
       :activity_type activity_type
       :unit unit
       :amount amount
       :kind kind})))

(defn query-activities
  [ds {:keys [date activity activity_type type]}]
  (let [raw-activities (db/query-activities-raw ds {:date date
                                                     :activity activity
                                                     :activity_type activity_type})]
    (->> raw-activities
         (map #(enrich-activity % type))
         (remove nil?))))

(defn plano-x-realizado
  [ds filters]
  (let [activities (query-activities ds filters)]
    {:items activities}))
