(ns challenge.api.db
  "Database operations and data access layer."
  (:require [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [clojure.string :as str]
            [clojure.tools.logging :as log]))

(defn- now []
  (java.time.OffsetDateTime/now))

(defn- ensure-local-date [v]
  (cond
    (instance? java.time.LocalDate v) v
    (instance? java.sql.Date v) (.toLocalDate ^java.sql.Date v)
    :else (try
            (let [date-str (str/trim (str v))]
              (log/debug "Converting date to LocalDate" {:input v :date-str date-str})
              (java.time.LocalDate/parse date-str))
            (catch Exception e
              (log/error e "Error converting date to LocalDate" {:input v :type (type v)})
              (throw (ex-info (str "Invalid date: " v) {:date v :error (.getMessage e)}))))))

(defn- find-activity
  [tx {:keys [date activity activity_type unit]}]
  (log/debug "Searching for activity" {:date date :activity activity :activity_type activity_type :unit unit})
  (let [params {:date (ensure-local-date date)
                :activity activity
                :activity_type activity_type
                :unit unit}
        result (first (sql/find-by-keys tx :activity params))]
    (log/debug "Activity search result" {:found (some? result)})
    result))

(defn- insert-activity
  [tx {:keys [date] :as row}]
  (log/debug "Inserting new activity" {:date date :activity (:activity row)})
  (let [payload (-> row
                    (assoc :date (ensure-local-date date))
                    (dissoc :amount))
        result (sql/insert! tx :activity (merge payload {:created_at (now)
                                                         :updated_at (now)}))]
    (log/debug "Activity inserted successfully")
    result))

(defn- update-activity-field
  [tx {:keys [date activity activity_type unit] :as row} field]
  (log/debug "Updating activity field" {:date date :activity activity :field field :amount (:amount row)})
  (let [payload {:date (ensure-local-date date)
                 :activity activity
                 :activity_type activity_type
                 :unit unit}
        values {field (:amount row)
                :updated_at (now)}
        result (sql/update! tx :activity values payload)]
    (log/debug "Activity field updated" {:rows-affected (first result)})
    result))

(defn- upsert-planned-tx!
  [tx row]
  (if (find-activity tx row)
    (update-activity-field tx row :amount_planned)
    (insert-activity tx (assoc row :amount_planned (:amount row)))))

(defn- upsert-executed-tx!
  [tx row]
  (if (find-activity tx row)
    (update-activity-field tx row :amount_executed)
    (insert-activity tx (assoc row :amount_executed (:amount row)))))

(defn import-planned-batch!
  "Imports a batch of planned activities.
  
  Parameters:
  - ds: Database datasource
  - rows: Vector of activity maps
  
  Returns:
  - Result of batch import operation"
  [ds rows]
  (let [start-time (System/currentTimeMillis)
        rows-count (count rows)]
    (log/info "Starting planned activities import" {:rows-count rows-count})
    (try
      (jdbc/with-transaction [tx ds]
        (doseq [row rows]
          (upsert-planned-tx! tx row)))
      (let [duration (- (System/currentTimeMillis) start-time)]
        (log/info "Planned activities import completed" {:rows-count rows-count :duration-ms duration}))
      (catch Exception e
        (let [duration (- (System/currentTimeMillis) start-time)]
          (log/error e "Error importing planned activities" {:rows-count rows-count :duration-ms duration})
          (throw e))))))

(defn import-executed-batch!
  "Imports a batch of executed activities.
  
  Parameters:
  - ds: Database datasource
  - rows: Vector of activity maps
  
  Returns:
  - Result of batch import operation"
  [ds rows]
  (let [start-time (System/currentTimeMillis)
        rows-count (count rows)]
    (log/info "Starting executed activities import" {:rows-count rows-count})
    (try
      (jdbc/with-transaction [tx ds]
        (doseq [row rows]
          (upsert-executed-tx! tx row)))
      (let [duration (- (System/currentTimeMillis) start-time)]
        (log/info "Executed activities import completed" {:rows-count rows-count :duration-ms duration}))
      (catch Exception e
        (let [duration (- (System/currentTimeMillis) start-time)]
          (log/error e "Error importing executed activities" {:rows-count rows-count :duration-ms duration})
          (throw e))))))

(defn query-activities-raw
  "Queries activities from database with filters.
  
  Parameters:
  - ds: Database datasource
  - filters: Map with :date, :activity (optional), :activity_type (optional)
  
  Returns:
  - Vector of activity maps with normalized keys"
  [ds {:keys [date activity activity_type]}]
  (let [start-time (System/currentTimeMillis)
        filters {:date date :activity activity :activity_type activity_type}]
    (log/info "Executing activities query" {:filters filters})
    (try
      (let [base-sql (str
                      "select activity, activity_type, unit, "
                      "amount_planned, amount_executed "
                      "from activity "
                      "where date = ?"
                      (when activity " and activity = ?")
                      (when activity_type " and activity_type = ?"))
            query-params (cond-> [(ensure-local-date date)]
                           activity (conj activity)
                           activity_type (conj activity_type))
            rows (jdbc/execute! ds (into [base-sql] query-params))
            normalized-rows (map (fn [row]
                                   (let [activity-key (or (:activity row) (:activity/activity row))
                                         activity-type-key (or (:activity_type row) (:activity/activity_type row))
                                         unit-key (or (:unit row) (:activity/unit row))
                                         amount-planned (or (:amount_planned row) (:activity/amount_planned row))
                                         amount-executed (or (:amount_executed row) (:activity/amount_executed row))]
                                     (log/debug "Normalizing record" {:row row
                                                                      :row-keys (keys row)
                                                                      :amount_planned amount-planned
                                                                      :amount_executed amount-executed
                                                                      :amount_planned-type (type amount-planned)
                                                                      :amount_executed-type (type amount-executed)})
                                     {:activity activity-key
                                      :activity_type activity-type-key
                                      :unit unit-key
                                      :amount_planned amount-planned
                                      :amount_executed amount-executed}))
                                 rows)
            duration (- (System/currentTimeMillis) start-time)
            sample-row (when (seq normalized-rows) (first normalized-rows))]
        (log/info "Activities query completed" {:filters filters :rows-count (count normalized-rows) :duration-ms duration})
        (when sample-row
          (log/debug "Sample record returned" {:sample-row sample-row
                                               :keys (keys sample-row)
                                               :amount_planned (:amount_planned sample-row)
                                               :amount_executed (:amount_executed sample-row)
                                               :amount_planned-type (type (:amount_planned sample-row))
                                               :amount_executed-type (type (:amount_executed sample-row))}))
        normalized-rows)
      (catch Exception e
        (let [duration (- (System/currentTimeMillis) start-time)]
          (log/error e "Error executing activities query" {:filters filters :duration-ms duration})
          (throw e))))))
