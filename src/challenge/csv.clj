(ns challenge.csv
  (:require
   [clojure.data.csv :as csv]
   [clojure.java.io :as io]
   [clojure.string :as string]
   [clojure.tools.logging :as log])
  (:import
   (java.io Reader)
   (java.time LocalDate)))

(defn detect-type-from-header
  [header]
  (let [columns (set header)]
    (cond
      (contains? columns "Amount planned") :planned
      (contains? columns "Amount executed") :executed
      :else (throw (ex-info "Invalid CSV type, header does not contain known amount column" {:header header})))))

(defn parse-number
  [s]
  (when (some? s)
    (try
      (bigdec s)
      (catch Exception _
        nil))))

(defn parse-date
  [s]
  (when (some? s)
    (try
      (LocalDate/parse s)
      (catch Exception _
        nil))))

(defn header-indexes
  [header]
  (zipmap header (range)))

(defn row->activity
  [idx line row]
  (let [date-idx (get idx "Date")
        activity-idx (get idx "Activity")
        type-idx (get idx "Activity type")
        unit-idx (get idx "Unit")
        amount-planned-idx (get idx "Amount planned")
        amount-executed-idx (get idx "Amount executed")
        amount-idx (or amount-planned-idx amount-executed-idx)
        date (nth row date-idx nil)
        activity (nth row activity-idx nil)
        activity-type (nth row type-idx nil)
        unit (nth row unit-idx nil)
        amount (nth row amount-idx nil)
        required-fields {"Date" date
                         "Activity" activity
                         "Activity type" activity-type
                         "Unit" unit
                         "Amount" amount}
        empty-fields (->> required-fields
                          (filter (fn [[_ v]] (or (nil? v) (string/blank? v))))
                          (map first)
                          (into []))]
    (if (seq empty-fields)
      {:error {:line line
               :reason (str "Required fields empty: " (string/join ", " empty-fields))}}
      (let [parsed-date (parse-date date)
            parsed-amount (parse-number amount)]
        (cond
          (nil? parsed-date) {:error {:line line
                                      :reason "Invalid date"}}
          (nil? parsed-amount) {:error {:line line
                                        :reason "Invalid amount"}}
          :else {:activity {:date parsed-date
                            :activity activity
                            :activity_type activity-type
                            :unit unit
                            :amount parsed-amount}})))))

(defn read-csv-reader
  ^java.util.List
  [^Reader r]
  (doall (csv/read-csv r)))

(defn parse-csv-reader
  [^Reader r]
  (let [start-time (System/currentTimeMillis)]
    (log/info "Starting CSV parse")
    (try
      (let [rows (read-csv-reader r)
            total-lines (count rows)
            header (first rows)
            body (next rows)
            kind (detect-type-from-header header)
            idx (header-indexes header)]
        (log/info "CSV read" {:total-lines total-lines :type kind :header header})
        (let [result (reduce (fn [acc [i row]]
                               (let [line (+ 2 i)
                                     row-result (row->activity idx line row)]
                                 (if-let [activity (:activity row-result)]
                                   (update acc :rows conj activity)
                                   (do
                                     (log/debug "Error processing CSV line" {:line line :error (:error row-result)})
                                     (update acc :errors conj (:error row-result))))))
                             {:rows [] :errors []}
                             (map-indexed vector body))
              duration (- (System/currentTimeMillis) start-time)
              parsed-result {:type kind
                            :rows (:rows result)
                            :errors (:errors result)}]
          (log/info "CSV parse completed" {:type kind
                                             :total-lines total-lines
                                             :valid (count (:rows result))
                                             :invalid (count (:errors result))
                                             :duration-ms duration})
          parsed-result))
      (catch Exception e
        (let [duration (- (System/currentTimeMillis) start-time)]
          (log/error e "Error parsing CSV" {:duration-ms duration})
          (throw e))))))

(defn parse-csv-file
  [path]
  (log/info "Starting CSV file parse" {:path path})
  (try
    (let [result (with-open [r (io/reader path)]
                   (parse-csv-reader r))]
      (log/info "CSV file parse completed" {:path path :type (:type result)})
      result)
    (catch Exception e
      (log/error e "Error parsing CSV file" {:path path})
      (throw e))))

