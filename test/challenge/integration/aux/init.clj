(ns challenge.integration.aux.init
  "Auxiliary functions for integration tests setup."
  (:require [challenge.api.system :as system]
            [com.stuartsierra.component :as component]
            [next.jdbc :as jdbc]
            [clojure.tools.logging :as log]))

(defrecord TestConfigComponent [value]
  component/Lifecycle
  (start [this]
    (log/info "Starting TestConfigComponent")
    (log/info "TestConfigComponent started successfully")
    this)
  (stop [this]
    (log/info "Stopping TestConfigComponent")
    this))

(defn- getenv [k default]
  (or (System/getenv k) default))

(defn- parse-int [s]
  (when s
    (try
      (Integer/parseInt (str s))
      (catch Exception _ nil))))

(defn create-test-system
  "Creates a test system with database configuration from environment or defaults.
  
  Uses environment variables:
  - DB_HOST (default: localhost)
  - DB_PORT (default: 5432)
  - DB_NAME (default: challenge_test)
  - DB_USER (default: postgres)
  - DB_PASSWORD (default: postgres)
  
  Returns:
  - Component system map"
  []
  (let [test-config {:database {:host (getenv "DB_HOST" "localhost")
                                 :port (or (some-> (getenv "DB_PORT" nil) parse-int) 5432)
                                 :name (getenv "DB_NAME" "challenge_test")
                                 :user (getenv "DB_USER" "postgres")
                                 :password (getenv "DB_PASSWORD" "postgres")}
                     :http {:port 0}}
        test-config-component (map->TestConfigComponent {:value test-config})]
    (system/new-system test-config-component)))

(defn ensure-test-database-exists
  "Creates the test database if it doesn't exist.
  
  Uses environment variables:
  - DB_HOST (default: localhost)
  - DB_PORT (default: 5432)
  - DB_NAME (default: challenge_test)
  - DB_USER (default: postgres)
  - DB_PASSWORD (default: postgres)
  
  Returns:
  - true if database exists or was created, false otherwise"
  []
  (let [host (getenv "DB_HOST" "localhost")
        port (or (some-> (getenv "DB_PORT" nil) parse-int) 5432)
        db-name (getenv "DB_NAME" "challenge_test")
        user (getenv "DB_USER" "postgres")
        password (getenv "DB_PASSWORD" "postgres")
        ;; Connect to postgres database to create test database
        admin-spec {:dbtype "postgresql"
                    :host host
                    :port port
                    :dbname "postgres"
                    :user user
                    :password password}]
    (try
      (let [admin-ds (jdbc/get-datasource admin-spec)
            ;; Check if database exists (using parameterized query for safety)
            db-exists? (jdbc/execute-one! admin-ds
                                          ["SELECT 1 FROM pg_database WHERE datname = ?" db-name])]
        (if db-exists?
          (do
            (log/info "Test database already exists" {:dbname db-name})
            true)
          (do
            (log/info "Creating test database" {:dbname db-name})
            ;; Note: PostgreSQL doesn't support parameters for database name in CREATE DATABASE
            ;; We validate db-name to only contain safe characters (alphanumeric and underscore)
            (if (re-matches #"^[a-zA-Z0-9_]+$" db-name)
              (do
                (jdbc/execute! admin-ds [(str "CREATE DATABASE " db-name)])
                (log/info "Test database created successfully" {:dbname db-name})
                true)
              (do
                (log/error "Invalid database name" {:dbname db-name})
                false)))))
      (catch Exception e
        (log/warn "Could not ensure test database exists" (.getMessage e))
        false))))

(defn check-database-available?
  "Checks if database is available for testing.
  
  Parameters:
  - ds: Database datasource
  
  Returns:
  - true if database is available, false otherwise"
  [ds]
  (try
    (jdbc/execute-one! ds ["SELECT 1"])
    true
    (catch Exception e
      (log/warn "Database not available for integration tests" (.getMessage e))
      false)))

(defn start-test-system
  "Starts a test system.
  
  First ensures the test database exists, then starts the system.
  
  Parameters:
  - test-system: Component system map
  
  Returns:
  - Started system"
  [test-system]
  (ensure-test-database-exists)
  (component/start-system test-system))

(defn stop-test-system
  "Stops a test system.
  
  Parameters:
  - test-system: Component system map"
  [test-system]
  (component/stop-system test-system))

(defn clear-database
  "Clears all data from the activity table.
  
  Parameters:
  - ds: Database datasource"
  [ds]
  (jdbc/execute! ds ["DELETE FROM activity"]))

(defn create-csv-file
  "Creates a temporary CSV file with the given content.
  
  Parameters:
  - content: String with CSV content
  
  Returns:
  - File object"
  [content]
  (let [temp-file (java.io.File/createTempFile "test" ".csv")]
    (spit temp-file content)
    temp-file))

(defn create-multipart-request
  "Creates a Ring request map for multipart file upload.
  
  Parameters:
  - file: File object
  - route: String with route path
  
  Returns:
  - Ring request map"
  [file route]
  {:request-method :post
   :uri route
   :multipart-params {"file" {:tempfile file
                              :filename "test.csv"
                              :content-type "text/csv"}}})

(defn create-query-request
  "Creates a Ring request map for query parameters.
  
  Parameters:
  - route: String with route path
  - query-params: Map with query parameters
  
  Returns:
  - Ring request map"
  [route query-params]
  {:request-method :get
   :uri route
   :query-params query-params})

(defn handler-request
  "Executes a request against a handler.
  
  Parameters:
  - handler: Ring handler function
  - request: Ring request map
  
  Returns:
  - Ring response map"
  [handler request]
  (handler request))

