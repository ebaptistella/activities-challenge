(ns challenge.adapters.common.date
  (:import [java.time Instant]
           [java.sql Timestamp]))

(defn instant->timestamp
  [instant]
  (when instant
    (if (instance? Instant instant)
      (Timestamp/from instant)
      instant)))

(defn timestamp->instant
  [timestamp]
  (when timestamp
    (if (instance? Timestamp timestamp)
      (.toInstant timestamp)
      timestamp)))

(defn convert-instants-to-timestamps
  [data]
  (reduce-kv
   (fn [acc k v]
     (assoc acc k (instant->timestamp v)))
   {}
   data))
