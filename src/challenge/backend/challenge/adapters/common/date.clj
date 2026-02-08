(ns challenge.adapters.common.date
  (:require [schema.core :as s])
  (:import [java.sql Timestamp]
           [java.time Instant]))

(s/defn instant->timestamp :- (s/maybe java.sql.Timestamp)
  [instant :- (s/maybe java.time.Instant)]
  (when instant
    (if (instance? Instant instant)
      (Timestamp/from instant)
      instant)))

(s/defn convert-instants-to-timestamps :- s/Any
  [data :- s/Any]
  (reduce-kv
   (fn [acc k v]
     (assoc acc k (instant->timestamp v)))
   {}
   data))
