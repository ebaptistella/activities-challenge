(ns challenge.infrastructure.persistency.activity
  (:require [challenge.adapters.activity :as adapters.activity]
            [challenge.components.persistency :as components.persistency]
            [challenge.models.activity :as models.activity]
            [clojure.string :as str]
            [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [schema.core :as s]))

(s/defn find-by-id :- (s/maybe models.activity/Activity)
  [activity-id :- s/Int
   persistency]
  (let [ds (components.persistency/get-datasource persistency)
        db-result (jdbc/execute-one! ds
                                     ["SELECT * FROM activity WHERE id = ?" activity-id])]
    (when db-result
      (adapters.activity/persistency->model db-result))))

(s/defn ^:private  build-list-where
  "Builds [where-clause params] for find-all from optional filters.
   Supports :date (exact), :activity (ILIKE %value%), :activity_type (exact)."
  [filters]
  (let [{:keys [date activity activity_type]} (or filters {})
        not-blank? (fn [v] (and (some? v) (not (str/blank? (str v)))))
        conditions (cond-> []
                     (not-blank? date) (conj ["date = CAST(? AS date)" date])
                     (not-blank? activity) (conj ["activity ILIKE ?" (str "%" (str activity) "%")])
                     (not-blank? activity_type) (conj ["activity_type = ?" (str activity_type)]))
        [clauses params] (when (seq conditions)
                           (reduce (fn [[clauses params] [clause param]]
                                     [(conj clauses clause) (conj params param)])
                                   [[] []] conditions))]
    (if (seq clauses)
      [(str/join " AND " clauses) params]
      [nil []])))

(s/defn find-all :- [models.activity/Activity]
  "Returns all activities, optionally filtered by date, activity (substring), and activity_type."
  ([persistency]
   (find-all persistency nil))
  ([persistency filters]
   (let [ds (components.persistency/get-datasource persistency)
         [where-clause params] (build-list-where filters)
         base-sql "SELECT * FROM activity"
         order-sql " ORDER BY date DESC, id DESC"
         full-sql (if where-clause
                    (str base-sql " WHERE " where-clause order-sql)
                    (str base-sql order-sql))
         query-vec (into [full-sql] params)
         db-results (sql/query ds query-vec)]
     (map adapters.activity/persistency->model db-results))))

(s/defn save! :- models.activity/Activity
  [activity :- models.activity/Activity
   persistency]
  (let [ds (components.persistency/get-datasource persistency)]
    (if (:id activity)
      ;; Update existing
      (let [activity-with-updated-at (assoc activity :updated-at (java.time.Instant/now))
            db-data (adapters.activity/model->db activity-with-updated-at)
            id (:id activity)]
        (sql/update! ds :activity (dissoc db-data :id) {:id id})
        (find-by-id id persistency))
      ;; Create new
      (let [now (java.time.Instant/now)
            activity-with-timestamps (assoc activity
                                            :created-at now
                                            :updated-at now)
            db-data (adapters.activity/model->db activity-with-timestamps)
            insert-data (into {} (remove (comp nil? second) db-data))
            columns (keys insert-data)
            placeholders (str/join ", " (repeat (count columns) "?"))
            column-names (str/join ", " (map name columns))
            values (vals insert-data)
            inserted-record (jdbc/execute-one! ds
                                               (into [(str "INSERT INTO activity (" column-names ") "
                                                           "VALUES (" placeholders ") "
                                                           "RETURNING *")]
                                                     values))]
        (when inserted-record
          (adapters.activity/persistency->model inserted-record))))))

(s/defn delete! :- s/Bool
  [activity-id :- s/Int
   persistency]
  (let [ds (components.persistency/get-datasource persistency)
        result (sql/delete! ds :activity {:id activity-id})]
    (> (first result) 0)))
