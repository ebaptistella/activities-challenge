(ns challenge.api.controllers.import
  "Controllers for orchestrating CSV import operations."
  (:require [challenge.api.adapters.adapters :as adapters]
            [challenge.api.infrastructure.csv :as csv]
            [challenge.api.infrastructure.database :as database]
            [challenge.api.logic.logic :as logic]
            [challenge.api.models.models :as models]
            [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]))

(defn extract-file-from-request
  "Extracts file from request (tries both :params and :multipart-params).
  
  Parameters:
  - request: Ring request map
  
  Returns:
  - File map with :tempfile key or nil"
  [request]
  (let [file (or (get-in request [:params "file"])
                 (get-in request [:multipart-params "file"]))]
    (:tempfile file)))

(defn import-csv!
  "Orchestrates CSV file import.
  
  Parameters:
  - ds: Database datasource
  - request: Ring request map with file upload
  
  Returns:
  - Ring response map with import summary or error"
  [ds request]
  (let [start-time (System/currentTimeMillis)
        tempfile (extract-file-from-request request)]
    (log/info "Starting import-csv!" {:has-file (some? tempfile)})
    (if (nil? tempfile)
      (do
        (log/warn "CSV file not sent in request")
        {:status 400
         :headers {"Content-Type" "application/json"}
         :body (json/write-str (models/error-response "CSV file not sent"))})
      (try
        (log/info "Processing CSV file for import")
        (with-open [r (io/reader tempfile)]
          (let [parsed (csv/parse-csv-reader r)
                {:keys [type rows errors]} parsed
                total-lines (+ (count rows) (count errors))]
            (log/info "CSV parsed" {:type type :total-lines total-lines :valid (count rows) :invalid (count errors)})
            (log/info "Starting database import" {:type type :rows-count (count rows)})
            (let [import-start-time (System/currentTimeMillis)]
              (case type
                :planned (database/import-planned-batch! ds rows)
                :executed (database/import-executed-batch! ds rows))
              (let [import-duration (- (System/currentTimeMillis) import-start-time)]
                (log/info "Import completed" {:type type :rows-count (count rows) :duration-ms import-duration})))
            (let [response-time (- (System/currentTimeMillis) start-time)
                  summary (logic/build-import-summary parsed)]
              (log/info "Import-csv! completed successfully" {:response-time-ms response-time :summary summary})
              {:status 200
               :headers {"Content-Type" "application/json"}
               :body (json/write-str (adapters/model->wire-response summary))})))
        (catch clojure.lang.ExceptionInfo e
          (let [response-time (- (System/currentTimeMillis) start-time)
                error-data (ex-data e)]
            (log/error e "Error processing import" {:response-time-ms response-time :error-data error-data})
            {:status 400
             :headers {"Content-Type" "application/json"}
             :body (json/write-str (models/error-response (.getMessage e) error-data))}))
        (catch Exception e
          (let [response-time (- (System/currentTimeMillis) start-time)]
            (log/error e "Unexpected error processing import" {:response-time-ms response-time})
            {:status 500
             :headers {"Content-Type" "application/json"}
             :body (json/write-str (models/error-response "Internal server error"))}))))))
