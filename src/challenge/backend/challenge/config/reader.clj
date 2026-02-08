(ns challenge.config.reader
  (:require [challenge.components.configuration :as components.configuration]
            [schema.core :as s]))

(def default-config-file
  "config/application.edn")

(def default-application-name
  "challenge")

(s/defn ^:private  get-config
  [config-component]
  (components.configuration/get-config config-component))

(s/defn ^:private  get-http-config
  [config-component]
  (get-in (get-config config-component) [:http]))

(s/defn http->port
  [config-component]
  (:port (get-http-config config-component)))

(s/defn http->host
  [config-component]
  (:host (get-http-config config-component)))

(s/defn database-config
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

(s/defn database-connection-uri
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

(s/defn database-connection-uri-from-component
  "Gets database config from component and builds JDBC connection URI.
   Checks DATABASE_URL environment variable first, then builds from config.
   Returns the connection URI string."
  [config-component]
  (or (System/getenv "DATABASE_URL")
      (database-connection-uri (database-config config-component))))