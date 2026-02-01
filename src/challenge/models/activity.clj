(ns challenge.models.activity
  (:require [schema.core :as s]))

(def activity-skeleton
  {:id {:schema s/Int
        :required false
        :doc "Activity unique identifier (auto-generated)"}
   :date {:schema java.time.LocalDate
          :required true
          :doc "Activity date"}
   :activity {:schema s/Str
              :required true
              :doc "Activity description"}
   :activity-type {:schema s/Str
                   :required true
                   :doc "Type of activity"}
   :unit {:schema s/Str
          :required true
          :doc "Unit of measurement"}
   :amount-planned {:schema (s/maybe s/Num)
                    :required false
                    :doc "Planned amount"}
   :amount-executed {:schema (s/maybe s/Num)
                     :required false
                     :doc "Executed amount"}
   :created-at {:schema (s/maybe java.time.Instant)
                :required false
                :doc "Creation timestamp"}
   :updated-at {:schema (s/maybe java.time.Instant)
                :required false
                :doc "Last update timestamp"}})

(defn strict-schema
  "Creates a strict schema from skeleton definition.
   Only allows keys defined in skeleton."
  [skeleton]
  (reduce-kv
   (fn [acc k v]
     (assoc acc k (:schema v)))
   {}
   skeleton))

(s/defschema Activity
  (strict-schema activity-skeleton))
