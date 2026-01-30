(ns challenge.api.http-server
  "HTTP server handlers (diplomat layer for external communication)."
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]
            [ring.middleware.multipart-params :as multipart]
            [ring.middleware.params :as params]
            [ring.util.response :as response]
            [reitit.ring :as reitit.ring]
            [challenge.api.adapters :as adapters]
            [challenge.api.logic :as logic]
            [challenge.api.models :as models]
            [challenge.api.csv :as csv]
            [challenge.api.db :as db]
            [challenge.api.domain :as domain]
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

(defn handle-import-success
  "Handles successful import processing.
  
  Parameters:
  - parsed: Map with parsed CSV data
  - start-time: Long with start timestamp
  
  Returns:
  - Ring response map with 200 status"
  [parsed start-time]
  (let [response-time (- (System/currentTimeMillis) start-time)
        summary (logic/build-import-summary parsed)]
    (log/info "Import completed successfully" {:response-time-ms response-time :summary summary})
    {:status 200
     :headers {"Content-Type" "application/json"}
     :body (json/write-str (adapters/model->wire-response summary))}))

(defn handle-import-error
  "Handles import error.
  
  Parameters:
  - error: Exception
  - start-time: Long with start timestamp
  - status-code: Integer HTTP status code
  
  Returns:
  - Ring response map with error status"
  [error start-time status-code]
  (let [response-time (- (System/currentTimeMillis) start-time)
        error-data (when (instance? clojure.lang.ExceptionInfo error)
                     (ex-data error))
        error-response (models/error-response
                        (.getMessage error)
                        error-data)]
    (log/error error "Error processing import" {:response-time-ms response-time :status-code status-code})
    {:status status-code
     :headers {"Content-Type" "application/json"}
     :body (json/write-str error-response)}))

(defn import-handler
  "Handles CSV file import request.
  
  Parameters:
  - ds: Database datasource
  - request: Ring request map
  
  Returns:
  - Ring response map"
  [ds request]
  (let [start-time (System/currentTimeMillis)
        tempfile (extract-file-from-request request)]
    (log/info "Starting import-handler" {:has-file (some? tempfile)})
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
                :planned (db/import-planned-batch! ds rows)
                :executed (db/import-executed-batch! ds rows))
              (let [import-duration (- (System/currentTimeMillis) import-start-time)]
                (log/info "Import completed" {:type type :rows-count (count rows) :duration-ms import-duration})))
            (handle-import-success parsed start-time)))
        (catch clojure.lang.ExceptionInfo e
          (handle-import-error e start-time 400))
        (catch Exception e
          (handle-import-error e start-time 500))))))

(defn handle-activities-success
  "Handles successful activities query.
  
  Parameters:
  - result: Map with :items vector
  - start-time: Long with start timestamp
  
  Returns:
  - Ring response map with 200 status"
  [result start-time]
  (let [response-time (- (System/currentTimeMillis) start-time)
        items-count (count (:items result))]
    (log/info "Activities-handler completed successfully" {:response-time-ms response-time :items-count items-count})
    {:status 200
     :headers {"Content-Type" "application/json"}
     :body (json/write-str (adapters/model->wire-response result))}))

(defn handle-activities-error
  "Handles activities query error.
  
  Parameters:
  - error: Exception
  - start-time: Long with start timestamp
  - filters: Map with query filters
  
  Returns:
  - Ring response map with error status"
  [error start-time filters]
  (let [response-time (- (System/currentTimeMillis) start-time)]
    (log/error error "Error processing activities-handler" {:response-time-ms response-time :filters filters})
    {:status 500
     :headers {"Content-Type" "application/json"}
     :body (json/write-str (models/error-response "Internal server error"))}))

(defn activities-handler
  "Handles activities query request.
  
  Parameters:
  - ds: Database datasource
  - request: Ring request map
  
  Returns:
  - Ring response map"
  [ds request]
  (let [start-time (System/currentTimeMillis)
        query-params (:query-params request)
        filters (logic/extract-query-filters query-params)]
    (log/info "Starting activities-handler" {:filters filters})
    (if (nil? filters)
      (do
        (log/warn "Parameter 'date' not provided in request")
        {:status 400
         :headers {"Content-Type" "application/json"}
         :body (json/write-str (models/error-response "Parameter 'date' is required"))})
      (try
        (log/info "Executing activities query" {:filters filters})
        (let [result (domain/plano-x-realizado ds filters)]
          (handle-activities-success result start-time))
        (catch Exception e
          (handle-activities-error e start-time filters))))))

(defn static-file-handler
  "Handles static file requests.
  
  Parameters:
  - request: Ring request map
  
  Returns:
  - Ring response map with file or 404"
  [request]
  (let [uri (:uri request)
        resource-path (if (= uri "/")
                        "public/index.html"
                        (str "public" uri))
        resource (response/resource-response resource-path)]
    (or resource
        {:status 404
         :headers {}
         :body "Not found"})))

(defn health-handler
  "Handles health check requests.
  
  Parameters:
  - request: Ring request map (unused)
  
  Returns:
  - Ring response map with 200 status"
  [_]
  (log/debug "Health check request")
  {:status 200 :headers {} :body "ok"})

(defn index-handler
  "Handles index page requests.
  
  Parameters:
  - request: Ring request map (unused)
  
  Returns:
  - Ring response map with index.html"
  [_]
  (log/debug "Serving index.html")
  (response/resource-response "public/index.html"))

(defn routes
  "Defines application routes.
  
  Parameters:
  - ds: Database datasource
  
  Returns:
  - Vector with route definitions"
  [ds]
  [["/health" {:get health-handler}]
   ["/" {:get index-handler}]
   ["/api/import" {:post (fn [req] (import-handler ds req))}]
   ["/api/activities" {:get (fn [req] (activities-handler ds req))}]])

(defn create-router
  "Creates Reitit router with routes.
  
  Parameters:
  - ds: Database datasource
  
  Returns:
  - Reitit router"
  [ds]
  (reitit.ring/router (routes ds)))

(defn create-handler
  "Creates Ring handler with middleware.
  
  Parameters:
  - router: Reitit router
  
  Returns:
  - Ring handler function"
  [router]
  (let [reitit-handler (reitit.ring/ring-handler router static-file-handler)]
    (-> reitit-handler
        multipart/wrap-multipart-params
        params/wrap-params)))
