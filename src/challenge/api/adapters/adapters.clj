(ns challenge.api.adapters.adapters
  "Adapters for data transformation between wire formats and models.")

(defn db-row->activity
  "Converts database row to activity model.
  
  Parameters:
  - row: Map from database query (may have namespaced or non-namespaced keys)
  
  Returns:
  - Map with normalized activity data"
  [row]
  {:activity (or (:activity row) (:activity/activity row))
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

(defn model->wire-response
  "Converts domain model to wire response format.
  
  Parameters:
  - model: Map with domain data
  
  Returns:
  - Map ready for JSON serialization"
  [model]
  model)
