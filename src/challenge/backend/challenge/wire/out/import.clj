(ns challenge.wire.out.import
  (:require [challenge.schema :as schema]
            [schema.core :as s]))

(def import-response-skeleton
  {:type   {:schema s/Str :required true :doc "Import type (e.g. planned, executed)"}
   :valid  {:schema s/Int :required true :doc "Count of successfully imported rows"}
   :invalid {:schema s/Int :required true :doc "Count of rows that failed validation"}})

(s/defschema ImportResponse
  (schema/strict-schema import-response-skeleton))
