(ns challenge.controllers.activity
  (:require [challenge.components.persistency :as components.persistency]
            [challenge.infrastructure.persistency.activity :as persistency.activity]
            [challenge.logic.activity :as logic.activity]
            [challenge.models.activity :as models.activity]
            [schema.core :as s]))

(s/defn create-activity! :- models.activity/Activity
  [activity-data :- models.activity/Activity
   persistency :- components.persistency/IPersistency]
  (let [validated-activity (logic.activity/validate-activity activity-data)]
    (persistency.activity/save! validated-activity persistency)))

(s/defn get-activity :- (s/maybe models.activity/Activity)
  [activity-id :- s/Int
   persistency :- components.persistency/IPersistency]
  (persistency.activity/find-by-id activity-id persistency))

(s/defn list-activities :- [models.activity/Activity]
  [persistency :- components.persistency/IPersistency]
  (persistency.activity/find-all persistency))

(s/defn update-activity! :- models.activity/Activity
  [activity-id :- s/Int
   updates :- {s/Keyword s/Any}
   persistency :- components.persistency/IPersistency]
  (let [existing-activity (persistency.activity/find-by-id activity-id persistency)]
    (when (nil? existing-activity)
      (throw (ex-info "Activity not found" {:activity-id activity-id})))

    (let [can-update? (logic.activity/can-update? existing-activity updates)]
      (when-not can-update?
        (throw (ex-info "Activity cannot be updated" {:activity-id activity-id :updates updates})))
      (let [non-nil-updates (into {} (remove (comp nil? second) updates))
            merged-activity (merge existing-activity non-nil-updates)
            validated-activity (logic.activity/validate-activity merged-activity)]
        (persistency.activity/save! validated-activity persistency)))))

(s/defn delete-activity! :- s/Bool
  [activity-id :- s/Int
   persistency :- components.persistency/IPersistency]
  (let [existing-activity (persistency.activity/find-by-id activity-id persistency)]
    (when (nil? existing-activity)
      (throw (ex-info "Activity not found" {:activity-id activity-id})))
    (persistency.activity/delete! activity-id persistency)))
