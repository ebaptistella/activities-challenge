(ns challenge.api.logic.logic
  "Pure business logic functions.")

(defn calculate-kind
  "Calculates the kind of activity based on planned and executed amounts.
  
  Parameters:
  - amount-planned: BigDecimal or nil
  - amount-executed: BigDecimal or nil
  
  Returns:
  - Keyword :both, :planned, :executed, or nil if both are nil"
  [amount-planned amount-executed]
  (cond
    (and (some? amount-planned) (some? amount-executed)) :both
    (some? amount-planned) :planned
    (some? amount-executed) :executed
    :else nil))

(defn select-relevant-amount
  "Selects the relevant amount based on type filter.
  
  Parameters:
  - amount-planned: BigDecimal or nil
  - amount-executed: BigDecimal or nil
  - type-filter: String \"planned\", \"executed\", or nil
  
  Returns:
  - BigDecimal or nil"
  [amount-planned amount-executed type-filter]
  (case type-filter
    "planned" amount-planned
    "executed" amount-executed
    (or amount-executed amount-planned)))

(defn enrich-activity
  "Enriches an activity row with calculated kind and selected amount.
  
  Parameters:
  - activity-row: Map with :activity, :activity_type, :unit, :amount_planned, :amount_executed
  - type-filter: String \"planned\", \"executed\", or nil
  
  Returns:
  - Map with enriched activity or nil if kind cannot be calculated"
  [activity-row type-filter]
  (let [activity (:activity activity-row)
        activity-type (:activity_type activity-row)
        unit (:unit activity-row)
        amount-planned (:amount_planned activity-row)
        amount-executed (:amount_executed activity-row)
        kind (calculate-kind amount-planned amount-executed)
        amount (select-relevant-amount amount-planned amount-executed type-filter)]
    (when kind
      {:activity activity
       :activity_type activity-type
       :unit unit
       :amount amount
       :kind (name kind)})))

(defn filter-activities-by-kind
  "Filters activities that have a valid kind.
  
  Parameters:
  - activities: Vector of activity maps
  - type-filter: String \"planned\", \"executed\", or nil
  
  Returns:
  - Vector of enriched activities with valid kind"
  [activities type-filter]
  (->> activities
       (map #(enrich-activity % type-filter))
       (remove nil?)))

(defn validate-date-parameter
  "Validates that date parameter is present and not empty.
  
  Parameters:
  - date: String or nil
  
  Returns:
  - String if valid, nil if invalid"
  [date]
  (when (and (some? date) (not (empty? (str date))))
    (str date)))

(defn extract-query-filters
  "Extracts and validates query parameters for activities query.
  
  Parameters:
  - query-params: Map with query parameters
  
  Returns:
  - Map with :date, :activity, :activity_type, :type or nil if date is invalid"
  [query-params]
  (let [date (validate-date-parameter (get query-params "date"))
        activity (get query-params "activity")
        activity-type (get query-params "activity_type")
        type (get query-params "type")]
    (when date
      {:date date
       :activity activity
       :activity_type activity-type
       :type type})))

(defn build-import-summary
  "Builds import summary from parsed CSV data.
  
  Parameters:
  - parsed: Map with :type, :rows, :errors
  
  Returns:
  - Map with import summary"
  [parsed]
  (let [{:keys [type rows errors]} parsed
        total (count rows)
        error-count (count errors)]
    {:type (name type)
     :valid total
     :invalid error-count
     :errors errors}))
