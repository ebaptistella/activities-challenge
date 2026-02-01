(ns challenge.wire.in.activity
  (:require [schema.core :as s]))

(defn loose-schema
  "Creates a loose schema from skeleton definition.
   Allows extra keys not defined in skeleton."
  [skeleton]
  (reduce-kv
   (fn [acc k v]
     (assoc acc (s/optional-key k) (:schema v)))
   {}
   skeleton))

(def activity-request-skeleton
  {:date {:schema s/Str
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
                     :doc "Executed amount"}})

(s/defschema ActivityRequest
  (loose-schema activity-request-skeleton))

(def activity-update-skeleton
  {:date {:schema (s/maybe s/Str)
          :required false
          :doc "Activity date as ISO string (YYYY-MM-DD)"}
   :activity {:schema (s/maybe s/Str)
              :required false
              :doc "Activity description"}
   :activity-type {:schema (s/maybe s/Str)
                   :required false
                   :doc "Type of activity"}
   :unit {:schema (s/maybe s/Str)
          :required false
          :doc "Unit of measurement"}
   :amount-planned {:schema (s/maybe s/Num)
                    :required false
                    :doc "Planned amount"}
   :amount-executed {:schema (s/maybe s/Num)
                     :required false
                     :doc "Executed amount"}})

(s/defschema ActivityUpdateRequest
  (loose-schema activity-update-skeleton))
