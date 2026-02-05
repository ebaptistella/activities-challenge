(ns challenge.wire.out.error
  (:require [challenge.schema :as schema]
            [schema.core :as s]))

(def error-response-skeleton
  {:error {:schema s/Str :required true :doc "Error message"}})

(s/defschema ErrorResponse
  (schema/strict-schema error-response-skeleton))
