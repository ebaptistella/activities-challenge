(ns challenge.wire.persistency.activity
  (:require [clj-schema.core :as schema]
            [schema.core :as s]))

(def activity-persistency-skeleton
  {:activity/id              {:schema (s/maybe s/Int)             :required false :doc "Activity unique identifier"}
   :activity/date            {:schema java.time.LocalDate         :required true  :doc "Activity date"}
   :activity/activity        {:schema s/Str                       :required true  :doc "Activity description"}
   :activity/activity-type   {:schema s/Str                       :required true  :doc "Type of activity"}
   :activity/unit            {:schema s/Str                       :required true  :doc "Unit of measurement"}
   :activity/amount-planned  {:schema (s/maybe s/Num)             :required false :doc "Planned amount"}
   :activity/amount-executed {:schema (s/maybe s/Num)             :required false :doc "Executed amount"}
   :activity/created-at      {:schema (s/maybe java.time.Instant) :required false :doc "Creation timestamp"}
   :activity/updated-at      {:schema (s/maybe java.time.Instant) :required false :doc "Last update timestamp"}})

(s/defschema ActivityPersistency
  (schema/strict-schema activity-persistency-skeleton))

(defn model->persistency
  "Converts Activity model to database format.
   Maps kebab-case keywords to snake_case column names for database."
  [activity]
  (into {}
        (remove (comp nil? second)
                {:id (:id activity)
                 :date (:date activity)
                 :activity (:activity activity)
                 :activity_type (:activity-type activity)
                 :unit (:unit activity)
                 :amount_planned (:amount-planned activity)
                 :amount_executed (:amount-executed activity)
                 :created_at (:created-at activity)
                 :updated_at (:updated-at activity)})))

(defn persistency->model
  "Converts database result (with snake_case keys) to Activity model.
   Maps database column names to kebab-case keywords."
  [db-result]
  {:id (:id db-result)
   :date (when-let [d (:date db-result)]
           (if (instance? java.sql.Date d)
             (.toLocalDate d)
             (if (string? d)
               (java.time.LocalDate/parse d)
               d)))
   :activity (:activity db-result)
   :activity-type (:activity_type db-result)
   :unit (:unit db-result)
   :amount-planned (:amount_planned db-result)
   :amount-executed (:amount_executed db-result)
   :created-at (when-let [ca (:created_at db-result)]
                 (if (instance? java.sql.Timestamp ca)
                   (.toInstant ca)
                   (if (string? ca)
                     (java.time.Instant/parse ca)
                     ca)))
   :updated-at (when-let [ua (:updated_at db-result)]
                 (if (instance? java.sql.Timestamp ua)
                   (.toInstant ua)
                   (if (string? ua)
                     (java.time.Instant/parse ua)
                     ua)))})
