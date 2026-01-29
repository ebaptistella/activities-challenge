(ns volis-challenge.csv
  (:require
   [clojure.data.csv :as csv]
   [clojure.java.io :as io])
  (:import
   (java.io Reader)
   (java.time LocalDate)))

(defn detect-type-from-header
  [header]
  (let [columns (set header)]
    (cond
      (contains? columns "Amount planned") :planned
      (contains? columns "Amount executed") :executed
      :else (throw (ex-info "Tipo de CSV inválido, header não contém coluna de amount conhecida" {:header header})))))

(defn parse-number
  [s]
  (when (some? s)
    (bigdec s)))

(defn parse-date
  [s]
  (when (some? s)
    (LocalDate/parse s)))

(defn header-indexes
  [header]
  (zipmap header (range)))

(defn row->activity
  [idx row]
  (let [date-idx (get idx "Date")
        activity-idx (get idx "Activity")
        type-idx (get idx "Activity type")
        unit-idx (get idx "Unit")
        amount-planned-idx (get idx "Amount planned")
        amount-executed-idx (get idx "Amount executed")
        amount-idx (or amount-planned-idx amount-executed-idx)]
    {:date (parse-date (nth row date-idx))
     :activity (nth row activity-idx)
     :activity_type (nth row type-idx)
     :unit (nth row unit-idx)
     :amount (parse-number (nth row amount-idx))}))

(defn read-csv-reader
  ^java.util.List
  [^Reader r]
  (doall (csv/read-csv r)))

(defn parse-csv-reader
  [^Reader r]
  (let [rows (read-csv-reader r)
        header (first rows)
        body (next rows)
        kind (detect-type-from-header header)
        idx (header-indexes header)
        activities (map (partial row->activity idx) body)]
    {:type kind
     :rows activities}))

(defn parse-csv-file
  [path]
  (with-open [r (io/reader path)]
    (parse-csv-reader r)))

