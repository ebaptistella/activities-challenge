(ns challenge.logic.import
  "Pure logic for CSV import: parse rows and produce activity maps for validation."
  (:require [challenge.adapters.activity :as adapters.activity]
            [challenge.logic.activity :as logic.activity]
            [clojure.data.csv :as csv]
            [clojure.string :as string])
  (:import [java.io StringReader]))

(defn- parse-number
  [s]
  (when (and s (not (string/blank? s)))
    (try
      (Double/parseDouble (string/trim s))
      (catch NumberFormatException _ nil))))

(defn- row->activity-request
  "Maps a CSV row (vector) to a wire-style map for planned import.
   Header order: Date, Activity, Activity type, Unit, Amount planned."
  [row]
  (when (and row (>= (count row) 5))
    (let [[date activity activity-type unit amount-planned] row]
      {:date (string/trim (str date))
       :activity (string/trim (str activity))
       :activity-type (string/trim (str activity-type))
       :unit (string/trim (str unit))
       :amount-planned (parse-number amount-planned)
       :amount-executed nil})))

(defn parse-csv-rows
  "Parses CSV string into a sequence of activity-request maps (planned format).
   Skips header and empty rows. Returns lazy seq."
  [csv-string]
  (when csv-string
    (let [reader (StringReader. csv-string)
          rows (csv/read-csv reader)
          data-rows (rest rows)]
      (->> data-rows
           (map row->activity-request)
           (filter some?)))))

(defn process-import-rows
  "Processes parsed activity requests: validate each, return counts and list of
   valid activity models for persistence. Does not perform I/O."
  [activity-requests current-date]
  (let [valid (atom [])
        invalid (atom 0)]
    (doseq [req activity-requests]
      (try
        (let [model (adapters.activity/wire->model req)
              validated (logic.activity/validate-activity model current-date)]
          (swap! valid conj validated))
        (catch Exception _
          (swap! invalid inc))))
    {:valid @valid :invalid @invalid}))
