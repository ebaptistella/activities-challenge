(ns volis-challenge.api
  (:require
   [clojure.java.io :as io]
   [clojure.data.json :as json]
   [reitit.ring :as reitit.ring]
   [ring.middleware.multipart-params :as multipart]
   [ring.middleware.params :as params]
   [ring.util.response :as response]
   [volis-challenge.csv :as csv]
   [volis-challenge.db :as db]
   [volis-challenge.domain :as domain]
   [clojure.tools.logging :as log]))

(defn import-summary
  [parsed]
  (let [{:keys [type rows errors]} parsed
        total (count rows)
        error-count (count errors)]
    {:type type
     :lines_read (+ total error-count)
     :valid total
     :invalid error-count
     :errors errors}))

(defn import-handler
  [ds request]
  (let [start-time (System/currentTimeMillis)
        ;; Try to get the file from :params or :multipart-params
        file (or (get-in request [:params "file"])
                 (get-in request [:multipart-params "file"]))
        tempfile (:tempfile file)]
    (log/info "Starting import-handler" {:has-params (some? (:params request))
                                         :params-keys (keys (:params request))
                                         :has-multipart-params (some? (:multipart-params request))
                                         :multipart-params-keys (keys (:multipart-params request))
                                         :has-file (some? file)
                                         :file-keys (when file (keys file))
                                         :has-tempfile (some? tempfile)
                                         :content-type (get-in request [:headers "content-type"])})
    (if (nil? tempfile)
      (do
        (log/warn "CSV file not sent in request" {:params (:params request)
                                                           :headers (:headers request)
                                                           :content-type (get-in request [:headers "content-type"])})
        {:status 400
         :headers {"Content-Type" "application/json"}
         :body (json/write-str {:error "CSV file not sent"})})
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
            (let [response-time (- (System/currentTimeMillis) start-time)
                  summary (import-summary parsed)]
              (log/info "Import-handler completed successfully" {:response-time-ms response-time :summary summary})
              {:status 200
               :headers {"Content-Type" "application/json"}
               :body (json/write-str summary)})))
        (catch clojure.lang.ExceptionInfo e
          (let [data (ex-data e)
                response-time (- (System/currentTimeMillis) start-time)]
            (log/error e "Error processing import" {:response-time-ms response-time :error-data data})
            {:status 400
             :headers {"Content-Type" "application/json"}
             :body (json/write-str {:error (.getMessage e)
                                    :details data})}))
        (catch Exception e
          (let [response-time (- (System/currentTimeMillis) start-time)]
            (log/error e "Unexpected error processing import" {:response-time-ms response-time})
            {:status 500
             :headers {"Content-Type" "application/json"}
             :body (json/write-str {:error "Internal server error"})}))))))

(defn activities-handler
  [ds request]
  (let [start-time (System/currentTimeMillis)
        query-params (:query-params request)
        date (get query-params "date")
        activity (get query-params "activity")
        activity-type (get query-params "activity_type")
        type (get query-params "type")
        filters {:date date :activity activity :activity_type activity-type :type type}]
    (log/info "Starting activities-handler" {:filters filters})
    (if (or (nil? date) (empty? (str date)))
      (do
        (log/warn "Parameter 'date' not provided in request")
        {:status 400
         :headers {"Content-Type" "application/json"}
         :body (json/write-str {:error "Parameter 'date' is required"})})
      (try
        (log/info "Executing activities query" {:filters filters})
        (let [result (domain/plano-x-realizado ds {:date (str date)
                                                   :activity activity
                                                   :activity_type activity-type
                                                   :type type})
              response-time (- (System/currentTimeMillis) start-time)
              items-count (count (:items result))
              sample-item (when (pos? items-count) (first (:items result)))]
          (log/info "Activities-handler completed successfully" {:response-time-ms response-time 
                                                               :items-count items-count
                                                               :sample-item sample-item
                                                               :result-keys (keys result)})
          (log/debug "Complete result" {:result result})
          {:status 200
           :headers {"Content-Type" "application/json"}
           :body (json/write-str result)})
        (catch Exception e
          (let [response-time (- (System/currentTimeMillis) start-time)]
            (log/error e "Error processing activities-handler" {:response-time-ms response-time :filters filters})
            {:status 500
             :headers {"Content-Type" "application/json"}
             :body (json/write-str {:error "Internal server error"})}))))))

(defn static-file-handler
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

(defn routes
  [ds]
  [["/health" {:get (fn [_]
                      (log/debug "Health check request")
                      {:status 200 :headers {} :body "ok"})}]
   ["/" {:get (fn [_]
                (log/debug "Serving index.html")
                (response/resource-response "public/index.html"))}]
   ["/api/import" {:post (fn [req] (import-handler ds req))}]
   ["/api/activities" {:get (fn [req] (activities-handler ds req))}]])

(defn create-router
  [ds]
  (reitit.ring/router (routes ds)))

(defn create-handler
  [router]
  (let [reitit-handler (reitit.ring/ring-handler router static-file-handler)]
    (-> reitit-handler
        multipart/wrap-multipart-params
        params/wrap-params)))

