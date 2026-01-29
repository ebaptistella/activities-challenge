(ns volis-challenge.db
  (:require
   [next.jdbc :as jdbc]
   [next.jdbc.sql :as sql]))

(def activity-key-columns
  [:date :activity :activity_type :unit])

(defn- now []
  (java.time.OffsetDateTime/now))

(defn- ensure-local-date [v]
  (cond
    (instance? java.time.LocalDate v) v
    (instance? java.sql.Date v) (.toLocalDate ^java.sql.Date v)
    :else (java.time.LocalDate/parse (str v))))

(defn- find-activity
  [tx {:keys [date activity activity_type unit]}]
  (let [params {:date (ensure-local-date date)
                :activity activity
                :activity_type activity_type
                :unit unit}]
    (first (sql/find-by-keys tx :activity params))))

(defn- insert-activity
  [tx {:keys [date activity activity_type unit] :as row}]
  (let [payload (-> row
                    (assoc :date (ensure-local-date date))
                    (dissoc :amount))]
    (sql/insert! tx :activity (merge payload {:created_at (now)
                                              :updated_at (now)}))))

(defn- update-activity-field
  [tx {:keys [date activity activity_type unit] :as row} field]
  (let [payload {:date (ensure-local-date date)
                 :activity activity
                 :activity_type activity_type
                 :unit unit}
        values {field (:amount row)
                :updated_at (now)}]
    (sql/update! tx :activity values payload))

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

  (defn upsert-planned!
    [ds row]
    (jdbc/with-transaction [tx ds]
      (upsert-planned-tx! tx row)))

  (defn upsert-executed!
    [ds row]
    (jdbc/with-transaction [tx ds]
      (upsert-executed-tx! tx row)))

  (defn import-planned-batch!
    [ds rows]
    (jdbc/with-transaction [tx ds]
      (doseq [row rows]
        (upsert-planned-tx! tx row))))

  (defn import-executed-batch!
    [ds rows]
    (jdbc/with-transaction [tx ds]
      (doseq [row rows]
        (upsert-executed-tx! tx row)))))

