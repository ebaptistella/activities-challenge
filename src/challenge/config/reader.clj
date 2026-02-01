(ns challenge.config.reader
  (:require [challenge.components.configuration :as components.configuration]))

(def default-config-file
  "config/application.edn")

(def default-application-name
  "challenge")

(defn- get-config
  [config-component]
  (components.configuration/get-config config-component))

(defn- get-http-config
  [config-component]
  (get-in (get-config config-component) [:http]))

(defn http->port
  [config-component]
  (:port (get-http-config config-component)))

(defn http->host
  [config-component]
  (:host (get-http-config config-component)))

(defn config-file
  "Returns the config file path from the config component.
   Falls back to default-config-file if not found in config."
  [config-component]
  (or (:config-file (get-config config-component))
      default-config-file))

(defn application-name
  "Returns the application name from the config component.
   Falls back to default-application-name if not found in config."
  [config-component]
  (or (:application-name (get-config config-component))
      default-application-name))

(defn database-config
  "Returns the database config from the config component.
   Applies environment variable overrides (DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD).
   Environment variables take precedence over config file values."
  [config-component]
  (let [base-db-config (get-in (get-config config-component) [:database])]
    (merge base-db-config
           {:host (or (System/getenv "DB_HOST") (:host base-db-config))
            :port (or (some-> (System/getenv "DB_PORT") #(Integer/parseInt %)) (:port base-db-config))
            :name (or (System/getenv "DB_NAME") (:name base-db-config))
            :user (or (System/getenv "DB_USER") (:user base-db-config))
            :password (or (System/getenv "DB_PASSWORD") (:password base-db-config))})))

(defn database-connection-uri
  "Builds a JDBC connection URI from database config.
   Returns nil if db-config is nil."
  [db-config]
  (when db-config
    (format "jdbc:postgresql://%s:%d/%s?user=%s&password=%s"
            (:host db-config)
            (:port db-config)
            (:name db-config)
            (:user db-config)
            (:password db-config))))

(defn database-connection-uri-from-component
  "Gets database config from component and builds JDBC connection URI.
   Checks DATABASE_URL environment variable first, then builds from config.
   Returns the connection URI string."
  [config-component]
  (or (System/getenv "DATABASE_URL")
      (database-connection-uri (database-config config-component))))