(ns challenge.wire.in.activity
  (:require [challenge.schema :as schema]
            [schema.core :as s]))

(def activity-request-skeleton
  {:date            {:schema s/Str           :required true  :doc "Activity date as ISO string (YYYY-MM-DD)"}
   :activity        {:schema s/Str           :required true  :doc "Activity description"}
   :activity-type   {:schema s/Str           :required true  :doc "Type of activity"}
   :unit            {:schema s/Str           :required true  :doc "Unit of measurement"}
   :amount-planned  {:schema (s/maybe s/Num) :required false :doc "Planned amount"}
   :amount-executed {:schema (s/maybe s/Num) :required false :doc "Executed amount"}})

(s/defschema ActivityRequest
  (schema/loose-schema activity-request-skeleton))

(def activity-update-skeleton
  {:date            {:schema (s/maybe s/Str) :required false :doc "Activity date as ISO string (YYYY-MM-DD)"}
   :activity        {:schema (s/maybe s/Str) :required false :doc "Activity description"}
   :activity-type   {:schema (s/maybe s/Str) :required false :doc "Type of activity"}
   :unit            {:schema (s/maybe s/Str) :required false :doc "Unit of measurement"}
   :amount-planned  {:schema (s/maybe s/Num) :required false :doc "Planned amount"}
   :amount-executed {:schema (s/maybe s/Num) :required false :doc "Executed amount"}})

(s/defschema ActivityUpdateRequest
  (schema/loose-schema activity-update-skeleton))
