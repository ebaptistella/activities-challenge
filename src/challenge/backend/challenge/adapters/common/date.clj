(ns challenge.adapters.common.date
  (:require [schema.core :as s])
  (:import [java.sql Timestamp]
           [java.time Instant]))

(s/defn instant->timestamp
  [instant]
  (when instant
    (if (instance? Instant instant)
      (Timestamp/from instant)
      instant)))

(s/defn convert-instants-to-timestamps
  [data]
  (reduce-kv
   (fn [acc k v]
     (assoc acc k (instant->timestamp v)))
   {}
   data))
