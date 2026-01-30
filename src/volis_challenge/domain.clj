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
  (let [filters {:date date :activity activity :activity_type activity_type :type type}]
    (log/info "Iniciando query-activities" {:filters filters})
    (try
      (let [raw-activities (db/query-activities-raw ds {:date date
                                                         :activity activity
                                                         :activity_type activity_type})
            enriched (->> raw-activities
                         (map #(enrich-activity % type))
                         (remove nil?))]
        (log/info "Query-activities concluída" {:filters filters :raw-count (count raw-activities) :enriched-count (count enriched)})
        enriched)
      (catch Exception e
        (log/error e "Erro ao executar query-activities" {:filters filters})
        (throw e)))))

(defn plano-x-realizado
  [ds filters]
  (let [start-time (System/currentTimeMillis)]
    (log/info "Iniciando plano-x-realizado" {:filters filters})
    (try
      (let [activities (query-activities ds filters)
            result {:items activities}
            duration (- (System/currentTimeMillis) start-time)]
        (log/info "Plano-x-realizado concluído" {:filters filters :items-count (count activities) :duration-ms duration})
        result)
      (catch Exception e
        (let [duration (- (System/currentTimeMillis) start-time)]
          (log/error e "Erro ao executar plano-x-realizado" {:filters filters :duration-ms duration})
          (throw e))))))
