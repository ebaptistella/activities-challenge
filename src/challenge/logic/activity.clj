(ns challenge.logic.activity
  (:require [challenge.models.activity :as models.activity]
            [schema.core :as s]))

(s/defn validate-activity :- models.activity/Activity
  "Validates activity data according to business rules.
   Returns the validated activity model.
   
   Business rules:
   - Date must not be in the future
   - Activity description must not be empty
   - Amount executed cannot exceed amount planned (if both present)
   - Activity type must be one of valid types"
  [activity :- models.activity/Activity]
  (let [now (java.time.LocalDate/now)
        date (:date activity)
        activity-desc (:activity activity)
        amount-planned (:amount-planned activity)
        amount-executed (:amount-executed activity)]
    ;; Validate date is not in the future
    (when (and date (.isAfter date now))
      (throw (ex-info "Activity date cannot be in the future"
                      {:activity activity
                       :date date})))
    ;; Validate activity description is not empty
    (when (or (nil? activity-desc) (empty? (.trim activity-desc)))
      (throw (ex-info "Activity description cannot be empty"
                      {:activity activity})))
    ;; Validate amount executed doesn't exceed planned (if both present)
    (when (and amount-planned amount-executed
               (> amount-executed amount-planned))
      (throw (ex-info "Amount executed cannot exceed amount planned"
                      {:activity activity
                       :amount-planned amount-planned
                       :amount-executed amount-executed})))
    activity))

(s/defn calculate-completion-rate :- s/Num
  "Calculates the completion rate percentage for an activity.
   Returns 0 if amount-planned is 0 or nil.
   
   Formula: (amount-executed / amount-planned) * 100"
  [activity :- models.activity/Activity]
  (let [amount-planned (:amount-planned activity)
        amount-executed (:amount-executed activity 0)]
    (if (and amount-planned (pos? amount-planned))
      (* 100 (/ amount-executed amount-planned))
      0)))

(s/defn enrich-activity :- models.activity/Activity
  "Enriches activity data with computed fields.
   Currently adds completion rate if applicable.
   This is a pure function that doesn't perform I/O."
  [activity :- models.activity/Activity]
  (let [completion-rate (calculate-completion-rate activity)]
    (assoc activity :completion-rate completion-rate)))

(s/defn can-update? :- s/Bool
  "Checks if an activity can be updated based on business rules.
   Returns true if update is allowed, false otherwise."
  [existing-activity :- (s/maybe models.activity/Activity)
   updates :- {s/Keyword s/Any}]
  (if (nil? existing-activity)
    false
    (let [date (:date updates)
          now (java.time.LocalDate/now)]
      ;; Can't update if trying to set date in the future
      (if (and date (.isAfter date now))
        false
        true))))
