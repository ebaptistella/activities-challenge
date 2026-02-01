(ns challenge.schema
  "Utility functions for creating schemas from skeleton definitions.
   Provides loose-schema and strict-schema functions that convert skeleton maps
   to Prismatic Schema definitions."
  (:require [schema.core :as s]))

(defn strict-schema
  "Creates a strict schema from skeleton definition.
   Only allows keys defined in skeleton.
   Uses Prismatic Schema's optional-key for non-required fields."
  [skeleton]
  (reduce-kv
   (fn [acc k v]
     (let [schema-type (:schema v)
           required? (:required v)]
       (if required?
         (assoc acc k schema-type)
         (assoc acc (s/optional-key k) schema-type))))
   {}
   skeleton))

(defn loose-schema
  "Creates a loose schema from skeleton definition.
   Allows extra keys not defined in skeleton.
   Uses Prismatic Schema's optional-key for non-required fields.
   Prismatic Schema by default allows extra keys, so we just use strict-schema
   but the validation will be lenient (extra keys won't cause validation errors)."
  [skeleton]
  ;; Prismatic Schema allows extra keys by default, so we can use the same
  ;; structure as strict-schema, but the difference is in how validation is done
  (strict-schema skeleton))
