(ns challenge.wire.out.health
  (:require [challenge.schema :as schema]
            [schema.core :as s]))

(def health-response-skeleton
  {:status  {:schema s/Str :required true :doc "Status of the service"}
   :service {:schema s/Str :required true :doc "Name of the service"}})

(s/defschema HealthResponse
  (schema/strict-schema health-response-skeleton))
