(ns challenge.wire.out.activity
  (:require [schema.core :as s]))

(defn strict-schema
  "Creates a strict schema from skeleton definition.
   Only allows keys defined in skeleton."
  [skeleton]
  (reduce-kv
   (fn [acc k v]
     (assoc acc k (:schema v)))
   {}
   skeleton))

(def activity-response-skeleton
  {:id {:schema s/Int
        :required true
        :doc "Activity unique identifier"}
   :date {:schema s/Str
          :required true
          :doc "Activity date as ISO string (YYYY-MM-DD)"}
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
   :created-at {:schema (s/maybe s/Str)
                :required false
                :doc "Creation timestamp as ISO string"}
   :updated-at {:schema (s/maybe s/Str)
                :required false
                :doc "Last update timestamp as ISO string"}})

(s/defschema ActivityResponse
  (strict-schema activity-response-skeleton))

(s/defschema ActivityListResponse
  {:activities {:schema [ActivityResponse]
                :required true
                :doc "List of activities"}})
