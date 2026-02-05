(ns challenge.logic.activity
  (:require [challenge.models.activity :as models.activity]
            [schema.core :as s]))

(s/defn validate-activity :- models.activity/Activity
  "Validates activity business rules.
  
  Parameters:
  - activity: Activity model to validate
  - current-date: Current date (java.time.LocalDate) for validation
  
  Returns:
  - Validated activity (same as input if valid)
  
  Throws:
  - ExceptionInfo if activity date is in the future
  - ExceptionInfo if activity description is empty
  - ExceptionInfo if amount executed exceeds amount planned"
  [activity :- models.activity/Activity
   current-date :- java.time.LocalDate]
  (let [date (:date activity)
        activity-desc (:activity activity)
        amount-planned (:amount-planned activity)
        amount-executed (:amount-executed activity)]
    (when (and date (.isAfter date current-date))
      (throw (ex-info "Activity date cannot be in the future"
                      {:activity activity
                       :date date
                       :current-date current-date})))
    (when (or (nil? activity-desc) (empty? (.trim activity-desc)))
      (throw (ex-info "Activity description cannot be empty"
                      {:activity activity})))
    (when (and amount-planned amount-executed
               (> amount-executed amount-planned))
      (throw (ex-info "Amount executed cannot exceed amount planned"
                      {:activity activity
                       :amount-planned amount-planned
                       :amount-executed amount-executed})))
    activity))

(s/defn can-update? :- s/Bool
  "Checks if an activity can be updated based on business rules.
  
  Parameters:
  - existing-activity: Existing activity model (may be nil)
  - updates: Map with fields to update
  - current-date: Current date (java.time.LocalDate) for validation
  
  Returns:
  - true if activity can be updated, false otherwise"
  [existing-activity :- (s/maybe models.activity/Activity)
   updates :- {s/Keyword s/Any}
   current-date :- java.time.LocalDate]
  (if (nil? existing-activity)
    false
    (let [date (:date updates)]
      (if (and date (.isAfter date current-date))
        false
        true))))
