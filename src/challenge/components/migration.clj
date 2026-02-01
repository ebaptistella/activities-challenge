(ns challenge.components.migration
  "Migration component that runs database migrations on system startup."
  (:require [challenge.config.reader :as config.reader]
            [challenge.components.logger :as logger]
            [migratus.core :as migratus]
            [com.stuartsierra.component :as component]))

(defrecord MigrationComponent [config logger]
  component/Lifecycle
  (start [this]
    (if (:migratus-config this)
      this
      (let [db-config (config.reader/database-config config)
            connection-uri (config.reader/database-connection-uri-from-component config)
            migratus-config {:store :database
                             :migration-dir "migrations"
                             :db {:connection-uri connection-uri}}
            start-time (System/currentTimeMillis)
            log (logger/bound logger)]
        (logger/log-call log :info "[Migration] Starting database migrations")
        (logger/log-call log :info "[Migration] Database: %s:%d/%s"
                         (:host db-config)
                         (:port db-config)
                         (:name db-config))
        (try
          (let [pending-count (count (migratus/pending-list migratus-config))]
            (if (pos? pending-count)
              (logger/log-call log :info "[Migration] Running %d pending migration(s)" pending-count)
              (logger/log-call log :info "[Migration] No pending migrations"))
            (migratus/migrate migratus-config)
            (logger/log-call log :info
                             "[Migration] Migrations completed successfully in %d ms"
                             (- (System/currentTimeMillis) start-time))
            (assoc this :migratus-config migratus-config))
          (catch Exception e
            (logger/log-call log :error
                             "[Migration] Error running migrations: %s"
                             (.getMessage e)
                             e)
            (throw e))))))
  (stop [this]
    (let [log (logger/bound logger)]
      (logger/log-call log :info "[Migration] Migration component stopped"))
    this))

(defn new-migration
  "Creates a new migration component."
  []
  (map->MigrationComponent {}))
