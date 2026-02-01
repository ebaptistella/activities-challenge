(ns challenge.infrastructure.persistency.activity
  (:require [challenge.components.persistency :as components.persistency]
            [challenge.models.activity :as models.activity]
            [challenge.wire.persistency.activity :as wire.persistency.activity]
            [next.jdbc.sql :as sql]
            [schema.core :as s]))

(s/defn find-by-id! :- (s/maybe models.activity/Activity)
  "Finds an activity by its ID.
   Returns the activity model or nil if not found."
  [activity-id :- s/Int
   persistency :- components.persistency/IPersistency]
  (let [ds (components.persistency/get-datasource persistency)
        result (sql/get-by-id ds :activity activity-id)]
    (when result
      (wire.persistency.activity/persistency->model result))))

(s/defn find-all! :- [models.activity/Activity]
  "Finds all activities.
   Returns a list of activity models."
  [persistency :- components.persistency/IPersistency]
  (let [ds (components.persistency/get-datasource persistency)
        results (sql/query ds ["SELECT * FROM activity ORDER BY date DESC, id DESC"])]
    (map wire.persistency.activity/persistency->model results)))

(s/defn find-by-date-range! :- [models.activity/Activity]
  "Finds activities within a date range.
   Returns a list of activity models."
  [start-date :- java.time.LocalDate
   end-date :- java.time.LocalDate
   persistency :- components.persistency/IPersistency]
  (let [ds (components.persistency/get-datasource persistency)
        results (sql/query ds
                           ["SELECT * FROM activity 
                             WHERE date >= ? AND date <= ?
                             ORDER BY date DESC, id DESC"
                            start-date end-date])]
    (map wire.persistency.activity/persistency->model results)))

(s/defn save! :- models.activity/Activity
  "Saves an activity (creates or updates).
   If activity has an ID, updates it; otherwise creates a new one.
   Returns the saved activity model."
  [activity :- models.activity/Activity
   persistency :- components.persistency/IPersistency]
  (let [ds (components.persistency/get-datasource persistency)
        persistency-data (wire.persistency.activity/model->persistency activity)]
    (if (:activity/id persistency-data)
      ;; Update existing
      (let [id (:activity/id persistency-data)
            update-data (dissoc persistency-data :activity/id)
            updated-data (assoc update-data :activity/updated-at (java.time.Instant/now))
            _ (sql/update! ds :activity updated-data {:id id})]
        (find-by-id! id persistency))
      ;; Create new
      (let [insert-data (dissoc persistency-data :activity/id)
            created-data (assoc insert-data
                                :activity/created-at (java.time.Instant/now)
                                :activity/updated-at (java.time.Instant/now))
            result (sql/insert! ds :activity created-data)
            new-id (:activity/id result)]
        (find-by-id! new-id persistency)))))

(s/defn delete! :- s/Bool
  "Deletes an activity by its ID.
   Returns true if deleted, false if not found."
  [activity-id :- s/Int
   persistency :- components.persistency/IPersistency]
  (let [ds (components.persistency/get-datasource persistency)
        result (sql/delete! ds :activity {:id activity-id})]
    (> (first result) 0)))
