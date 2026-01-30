(ns challenge.api.adapters.adapters
  "Adapters for data transformation between wire formats and models.")

(defn db-row->activity
  "Converts database row to activity model.
  
  Parameters:
  - row: Map from database query (may have namespaced or non-namespaced keys)
  
  Returns:
  - Map with normalized activity data"
  [row]
  {:date (or (:date row) (:activity/date row))
   :activity (or (:activity row) (:activity/activity row))
   :activity_type (or (:activity_type row) (:activity/activity_type row))
   :unit (or (:unit row) (:activity/unit row))
   :amount_planned (or (:amount_planned row) (:activity/amount_planned row))
   :amount_executed (or (:amount_executed row) (:activity/amount_executed row))})

(defn csv-row->activity
  "Converts parsed CSV row to activity model.
  
  Parameters:
  - csv-activity: Map from CSV parser with :date, :activity, :activity_type, :unit, :amount
  
  Returns:
  - Map with activity data ready for database"
  [csv-activity]
  csv-activity)

(defn wire->query-filters
  "Converts wire query parameters to domain query format.
  
  Parameters:
  - query-params: Map with string keys from HTTP request
  
  Returns:
  - Map with keyword keys for domain layer"
  [query-params]
  {:date (get query-params "date")
   :activity (get query-params "activity")
   :activity_type (get query-params "activity_type")
   :type (get query-params "type")})

(defn- format-date
  "Formats a date value for JSON serialization.
  
  Parameters:
  - date: LocalDate, java.sql.Date, or string
  
  Returns:
  - String in ISO format (YYYY-MM-DD)"
  [date]
  (cond
    (instance? java.time.LocalDate date)
    (str date)
    (instance? java.sql.Date date)
    (str (.toLocalDate ^java.sql.Date date))
    :else
    (str date)))

(defn model->wire-response
  "Converts domain model to wire response format.
  
  Parameters:
  - model: Map with domain data
  
  Returns:
  - Map ready for JSON serialization with dates and BigDecimal values formatted"
  [model]
  (if (contains? model :items)
    ;; Activities response
    (update model :items
            (fn [items]
              (mapv (fn [item]
                      (-> item
                          (update :date format-date)
                          ;; BigDecimal values will be serialized as numbers by clojure.data.json
                          ;; which is fine for JSON, but tests may need to compare differently
                      )) items)))
    model))
