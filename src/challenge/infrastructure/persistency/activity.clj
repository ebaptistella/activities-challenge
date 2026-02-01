(ns challenge.infrastructure.persistency.activity
  (:require [challenge.adapters.activity :as adapters.activity]
            [challenge.components.persistency :as components.persistency]
            [challenge.models.activity :as models.activity]
            [next.jdbc.sql :as sql]
            [schema.core :as s]))

(s/defn find-by-id :- (s/maybe models.activity/Activity)
  [activity-id :- s/Int
   persistency :- components.persistency/IPersistency]
  (let [ds (components.persistency/get-datasource persistency)
        db-result (sql/get-by-id ds :activity activity-id)]
    (when db-result
      (adapters.activity/persistency->model db-result))))

(s/defn find-all :- [models.activity/Activity]
  [persistency :- components.persistency/IPersistency]
  (let [ds (components.persistency/get-datasource persistency)
        db-results (sql/query ds ["SELECT * FROM activity ORDER BY date DESC, id DESC"])]
    (map adapters.activity/persistency->model db-results)))

(s/defn save! :- models.activity/Activity
  [activity :- models.activity/Activity
   persistency :- components.persistency/IPersistency]
  (let [ds (components.persistency/get-datasource persistency)
        db-data (adapters.activity/model->persistency activity)]
    (if (:id activity)
      ;; Update existing
      (let [id (:id activity)
            update-data (dissoc db-data :id)
            updated-data (assoc update-data :updated_at (java.time.Instant/now))
            _ (sql/update! ds :activity updated-data {:id id})]
        (find-by-id id persistency))
      ;; Create new
      (let [insert-data (dissoc db-data :id)
            created-data (assoc insert-data
                                :created_at (java.time.Instant/now)
                                :updated_at (java.time.Instant/now))
            result (sql/insert! ds :activity created-data)
            new-id (:id result)]
        (find-by-id new-id persistency)))))

(s/defn delete! :- s/Bool
  [activity-id :- s/Int
   persistency :- components.persistency/IPersistency]
  (let [ds (components.persistency/get-datasource persistency)
        result (sql/delete! ds :activity {:id activity-id})]
    (> (first result) 0)))
