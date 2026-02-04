(ns challenge.wire.out.activity
  (:require [challenge.schema :as schema]
            [schema.core :as s]))

(def activity-response-skeleton
  {:id              {:schema s/Int           :required true  :doc "Activity unique identifier"}
   :date            {:schema s/Str           :required true  :doc "Activity date as ISO string (YYYY-MM-DD)"}
   :activity        {:schema s/Str           :required true  :doc "Activity description"}
   :activity-type   {:schema s/Str           :required true  :doc "Type of activity"}
   :unit            {:schema s/Str           :required true  :doc "Unit of measurement"}
   :amount-planned  {:schema (s/maybe s/Num) :required false :doc "Planned amount"}
   :amount-executed {:schema (s/maybe s/Num) :required false :doc "Executed amount"}
   :created-at      {:schema (s/maybe s/Str) :required false :doc "Creation timestamp as ISO string"}
   :updated-at      {:schema (s/maybe s/Str) :required false :doc "Last update timestamp as ISO string"}})

(s/defschema ActivityResponse
  (schema/strict-schema activity-response-skeleton))

(def list-activities-response-skeleton
  {:activities {:schema [ActivityResponse] :required true :doc "List of activities"}})

(s/defschema ListActivitiesResponse
  (schema/strict-schema list-activities-response-skeleton))
