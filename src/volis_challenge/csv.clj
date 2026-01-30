(ns volis-challenge.csv
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
      :else (throw (ex-info "Tipo de CSV inválido, header não contém coluna de amount conhecida" {:header header})))))

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
               :reason (str "Campos obrigatorios vazios: " (string/join ", " empty-fields))}}
      (let [parsed-date (parse-date date)
            parsed-amount (parse-number amount)]
        (cond
          (nil? parsed-date) {:error {:line line
                                      :reason "Data invalida"}}
          (nil? parsed-amount) {:error {:line line
                                        :reason "Amount invalido"}}
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
    (log/info "Iniciando parse do CSV")
    (try
      (let [rows (read-csv-reader r)
            total-lines (count rows)
            header (first rows)
            body (next rows)
            kind (detect-type-from-header header)
            idx (header-indexes header)]
        (log/info "CSV lido" {:total-lines total-lines :type kind :header header})
        (let [result (reduce (fn [acc [i row]]
                               (let [line (+ 2 i)
                                     row-result (row->activity idx line row)]
                                 (if-let [activity (:activity row-result)]
                                   (update acc :rows conj activity)
                                   (do
                                     (log/debug "Erro ao processar linha do CSV" {:line line :error (:error row-result)})
                                     (update acc :errors conj (:error row-result))))))
                             {:rows [] :errors []}
                             (map-indexed vector body))
              duration (- (System/currentTimeMillis) start-time)
              parsed-result {:type kind
                            :rows (:rows result)
                            :errors (:errors result)}]
          (log/info "Parse do CSV concluído" {:type kind
                                             :total-lines total-lines
                                             :valid (count (:rows result))
                                             :invalid (count (:errors result))
                                             :duration-ms duration})
          parsed-result))
      (catch Exception e
        (let [duration (- (System/currentTimeMillis) start-time)]
          (log/error e "Erro ao fazer parse do CSV" {:duration-ms duration})
          (throw e))))))

(defn parse-csv-file
  [path]
  (log/info "Iniciando parse de arquivo CSV" {:path path})
  (try
    (let [result (with-open [r (io/reader path)]
                   (parse-csv-reader r))]
      (log/info "Parse de arquivo CSV concluído" {:path path :type (:type result)})
      result)
    (catch Exception e
      (log/error e "Erro ao fazer parse de arquivo CSV" {:path path})
      (throw e))))

