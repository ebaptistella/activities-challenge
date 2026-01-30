(ns challenge.models
  "Data models and domain entities.")

(defn activity
  "Creates an activity model.
  
  Parameters:
  - date: LocalDate
  - activity: String
  - activity-type: String
  - unit: String
  - amount: BigDecimal
  
  Returns:
  - Map with activity data"
  [date activity activity-type unit amount]
  {:date date
   :activity activity
   :activity_type activity-type
   :unit unit
   :amount amount})

(defn activity-with-amounts
  "Creates an activity model with planned and executed amounts.
  
  Parameters:
  - date: LocalDate
  - activity: String
  - activity-type: String
  - unit: String
  - amount-planned: BigDecimal or nil
  - amount-executed: BigDecimal or nil
  
  Returns:
  - Map with activity data including amounts"
  [date activity activity-type unit amount-planned amount-executed]
  {:date date
   :activity activity
   :activity_type activity-type
   :unit unit
   :amount_planned amount-planned
   :amount_executed amount-executed})

(defn enriched-activity
  "Creates an enriched activity model with calculated kind and selected amount.
  
  Parameters:
  - activity: String
  - activity-type: String
  - unit: String
  - amount: BigDecimal or nil
  - kind: Keyword (:planned, :executed, or :both)
  
  Returns:
  - Map with enriched activity data"
  [activity activity-type unit amount kind]
  {:activity activity
   :activity_type activity-type
   :unit unit
   :amount amount
   :kind kind})

(defn import-summary
  "Creates an import summary model.
  
  Parameters:
  - type: Keyword (:planned or :executed)
  - valid: Integer count of valid rows
  - invalid: Integer count of invalid rows
  - errors: Vector of error maps
  
  Returns:
  - Map with import summary data"
  [type valid invalid errors]
  {:type type
   :valid valid
   :invalid invalid
   :errors errors})

(defn activities-response
  "Creates an activities API response model.
  
  Parameters:
  - items: Vector of enriched activity maps
  
  Returns:
  - Map with items array"
  [items]
  {:items items})

(defn error-response
  "Creates an error response model.
  
  Parameters:
  - error: String with error message
  - details: Optional map with error details
  
  Returns:
  - Map with error response data"
  [error & [details]]
  (if details
    {:error error :details details}
    {:error error}))
