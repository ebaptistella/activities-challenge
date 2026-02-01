(ns challenge.logic.activity
  (:require [challenge.models.activity :as models.activity]
            [schema.core :as s]))

(s/defn validate-activity :- models.activity/Activity
  [activity :- models.activity/Activity]
  (let [now (java.time.LocalDate/now)
        date (:date activity)
        activity-desc (:activity activity)
        amount-planned (:amount-planned activity)
        amount-executed (:amount-executed activity)]
    (when (and date (.isAfter date now))
      (throw (ex-info "Activity date cannot be in the future"
                      {:activity activity
                       :date date})))
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
  [existing-activity :- (s/maybe models.activity/Activity)
   updates :- {s/Keyword s/Any}]
  (if (nil? existing-activity)
    false
    (let [date (:date updates)
          now (java.time.LocalDate/now)]
      (if (and date (.isAfter date now))
        false
        true))))
