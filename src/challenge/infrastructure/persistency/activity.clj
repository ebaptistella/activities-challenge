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
      (let [id (:activity/id db-data)
            update-data (dissoc db-data :activity/id)
            updated-data (assoc update-data :activity/updated-at (java.time.Instant/now))
            ;; Convert namespaced keys to snake_case for database
            db-update-data (into {} (map (fn [[k v]]
                                           [(keyword (name k)) v])
                                         updated-data))]
        (sql/update! ds :activity db-update-data {:id id})
        (find-by-id id persistency))
      ;; Create new
      (let [insert-data (dissoc db-data :activity/id)
            created-data (assoc insert-data
                                :activity/created-at (java.time.Instant/now)
                                :activity/updated-at (java.time.Instant/now))
            ;; Convert namespaced keys to snake_case for database
            db-insert-data (into {} (map (fn [[k v]]
                                           [(keyword (name k)) v])
                                         created-data))
            result (sql/insert! ds :activity db-insert-data)
            new-id (:id result)]
        (find-by-id new-id persistency)))))

(s/defn delete! :- s/Bool
  [activity-id :- s/Int
   persistency :- components.persistency/IPersistency]
  (let [ds (components.persistency/get-datasource persistency)
        result (sql/delete! ds :activity {:id activity-id})]
    (> (first result) 0)))
