(ns challenge.wire.persistency.activity
  (:require [challenge.schema :as schema]
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

(def activity-db-result-skeleton
  {:id              {:schema (s/maybe s/Int) :required false :doc "Activity unique identifier"}
   :date            {:schema (s/maybe s/Any) :required false :doc "Activity date (LocalDate, java.sql.Date, string)"}
   :activity        {:schema (s/maybe s/Str) :required false :doc "Activity description"}
   :activity_type   {:schema (s/maybe s/Str) :required false :doc "Type of activity (snake_case from DB)"}
   :unit            {:schema (s/maybe s/Str) :required false :doc "Unit of measurement"}
   :amount_planned  {:schema (s/maybe s/Num) :required false :doc "Planned amount"}
   :amount_executed {:schema (s/maybe s/Num) :required false :doc "Executed amount"}
   :created_at      {:schema (s/maybe s/Any) :required false :doc "Creation timestamp"}
   :updated_at      {:schema (s/maybe s/Any) :required false :doc "Last update timestamp"}})

(s/defschema ActivityDbResult
  (schema/loose-schema activity-db-result-skeleton))

(s/defschema ActivityPersistencyInput
  (s/conditional #(contains? % :activity/id) ActivityPersistency
                 :else ActivityDbResult))
