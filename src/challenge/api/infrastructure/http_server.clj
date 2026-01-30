(ns challenge.api.diplomat.http-server
  "HTTP server handlers (infrastructure layer for external communication)."
  (:require [clojure.data.json :as json]
            [ring.middleware.multipart-params :as multipart]
            [ring.middleware.params :as params]
            [ring.util.response :as response]
            [reitit.ring :as reitit.ring]
            [challenge.api.controllers :as controllers]
            [clojure.tools.logging :as log]))

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

(defn import-handler
  "Handles CSV file import request.
  
  Parameters:
  - ds: Database datasource
  - request: Ring request map
  
  Returns:
  - Ring response map"
  [ds request]
  (let [start-time (System/currentTimeMillis)]
    (log/info "Starting import-handler")
    (try
      (let [result (controllers/import-csv! ds request)
            response-time (- (System/currentTimeMillis) start-time)]
        (log/info "Import-handler completed" {:response-time-ms response-time})
        result)
      (catch Exception e
        (let [response-time (- (System/currentTimeMillis) start-time)]
          (log/error e "Error in import-handler" {:response-time-ms response-time})
          {:status 500
           :headers {"Content-Type" "application/json"}
           :body (json/write-str {:error "Internal server error"})})))))

(defn activities-handler
  "Handles activities query request.
  
  Parameters:
  - ds: Database datasource
  - request: Ring request map
  
  Returns:
  - Ring response map"
  [ds request]
  (let [start-time (System/currentTimeMillis)]
    (log/info "Starting activities-handler")
    (try
      (let [result (controllers/query-activities! ds request)
            response-time (- (System/currentTimeMillis) start-time)]
        (log/info "Activities-handler completed" {:response-time-ms response-time})
        result)
      (catch Exception e
        (let [response-time (- (System/currentTimeMillis) start-time)]
          (log/error e "Error in activities-handler" {:response-time-ms response-time})
          {:status 500
           :headers {"Content-Type" "application/json"}
           :body (json/write-str {:error "Internal server error"})})))))

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
