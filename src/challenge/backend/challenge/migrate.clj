(ns challenge.migrate
  "Standalone migration runner. Can be executed via: lein run -m challenge.migrate"
  (:gen-class)
  (:require [challenge.components.configuration :as components.configuration]
            [challenge.config.reader :as config.reader]
            [com.stuartsierra.component :as component]
            [migratus.core :as migratus]
            [schema.core :as s]))

(s/defn run-migrations
  "Runs database migrations using the configuration from application.edn.
   Environment variables can override config values:
   - DATABASE_URL: Full JDBC connection string (takes precedence)
   - DB_HOST: Database host (defaults to config value)
   - DB_PORT: Database port (defaults to config value)
   - DB_NAME: Database name (defaults to config value)
   - DB_USER: Database user (defaults to config value)
   - DB_PASSWORD: Database password (defaults to config value)"
  []
  (println "[Migration] Starting migration process...")
  (let [config-component (components.configuration/new-config "config/application.edn")
        _ (component/start config-component)
        db-config (config.reader/database-config config-component)
        connection-uri (config.reader/database-connection-uri-from-component config-component)
        migratus-config {:store :database
                         :migration-dir "migrations"
                         :db {:connection-uri connection-uri}}
        start-time (System/currentTimeMillis)]
    (println (format "[Migration] Using connection: jdbc:postgresql://%s:%d/%s"
                     (:host db-config) (:port db-config) (:name db-config)))
    (try
      (migratus/migrate migratus-config)
      (let [duration (- (System/currentTimeMillis) start-time)]
        (println (format "[Migration] ✓ Migrations completed successfully in %d ms" duration)))
      (finally
        (component/stop config-component)))))

(s/defn -main
  "Main entry point for running migrations standalone."
  [& _args]
  (try
    (run-migrations)
    (System/exit 0)
    (catch Exception e
      (println (format "[Migration] ✗ Error running migrations: %s" (.getMessage e)))
      (.printStackTrace e)
      (System/exit 1))))
