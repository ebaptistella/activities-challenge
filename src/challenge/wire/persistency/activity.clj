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
