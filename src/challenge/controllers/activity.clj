(ns challenge.controllers.activity
  (:require [challenge.components.persistency :as components.persistency]
            [challenge.infrastructure.persistency.activity :as persistency.activity]
            [challenge.logic.activity :as logic.activity]
            [challenge.models.activity :as models.activity]
            [schema.core :as s]))

(s/defn create-activity! :- models.activity/Activity
  "Creates a new activity.
   Follows Logic Sandwich pattern: Queries → Logic → Effects.
   
   Returns the created activity model."
  [activity-data :- models.activity/Activity
   persistency :- components.persistency/IPersistency]
  ;; 1. QUERIES (if needed - none for creation)
  ;; 2. LOGIC (pure business logic)
  (let [validated-activity (logic.activity/validate-activity activity-data)]
    ;; 3. EFFECTS (atomic write)
    (persistency.activity/save! validated-activity persistency)))

(s/defn get-activity! :- (s/maybe models.activity/Activity)
  "Gets an activity by ID.
   Follows Logic Sandwich pattern: Queries → Logic → Effects.
   
   Returns the activity model or nil if not found."
  [activity-id :- s/Int
   persistency :- components.persistency/IPersistency]
  ;; 1. QUERIES
  (persistency.activity/find-by-id! activity-id persistency))

(s/defn list-activities! :- [models.activity/Activity]
  "Lists all activities.
   Follows Logic Sandwich pattern: Queries → Logic → Effects.
   
   Returns a list of activity models."
  [persistency :- components.persistency/IPersistency]
  ;; 1. QUERIES
  (persistency.activity/find-all! persistency))

(s/defn update-activity! :- models.activity/Activity
  "Updates an existing activity.
   Follows Logic Sandwich pattern: Queries → Logic → Effects.
   
   Returns the updated activity model."
  [activity-id :- s/Int
   updates :- {s/Keyword s/Any}
   persistency :- components.persistency/IPersistency]
  ;; 1. QUERIES
  (let [existing-activity (persistency.activity/find-by-id! activity-id persistency)]
    (when (nil? existing-activity)
      (throw (ex-info "Activity not found" {:activity-id activity-id})))
    ;; 2. LOGIC
    (let [can-update? (logic.activity/can-update? existing-activity updates)]
      (when-not can-update?
        (throw (ex-info "Activity cannot be updated" {:activity-id activity-id :updates updates})))
      (let [non-nil-updates (into {} (remove (comp nil? second) updates))
            merged-activity (merge existing-activity non-nil-updates)
            validated-activity (logic.activity/validate-activity merged-activity)]
        ;; 3. EFFECTS (atomic write)
        (persistency.activity/save! validated-activity persistency)))))

(s/defn delete-activity! :- s/Bool
  "Deletes an activity by ID.
   Follows Logic Sandwich pattern: Queries → Logic → Effects.
   
   Returns true if deleted, false if not found."
  [activity-id :- s/Int
   persistency :- components.persistency/IPersistency]
  ;; 1. QUERIES
  (let [existing-activity (persistency.activity/find-by-id! activity-id persistency)]
    (when (nil? existing-activity)
      (throw (ex-info "Activity not found" {:activity-id activity-id})))
    ;; 2. LOGIC (no business logic needed for deletion)
    ;; 3. EFFECTS
    (persistency.activity/delete! activity-id persistency)))
