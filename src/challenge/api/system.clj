(ns challenge.api.system
  (:require [challenge.api.config :as config]
            [com.stuartsierra.component :as component]
            [migratus.core :as migratus]
            [next.jdbc :as jdbc]
            [ring.adapter.jetty :as jetty]
            [challenge.api.http-server :as http-server]
            [clojure.tools.logging :as log]))

(defn- jdbc-spec-from-config [cfg]
  (let [db (:database cfg)]
    {:dbtype "postgresql"
     :host (:host db)
     :port (:port db)
     :dbname (:name db)
     :user (:user db)
     :password (:password db)}))

(defn- connection-uri-from-config [cfg]
  (let [db (:database cfg)]
    (str "jdbc:postgresql://"
         (:host db)
         ":"
         (:port db)
         "/"
         (:name db)
         "?user="
         (:user db)
         "&password="
         (:password db))))

(defn- http-port-from-config [cfg]
  (get-in cfg [:http :port]))

(defrecord ConfigComponent []
  component/Lifecycle
  (start [this]
    (log/info "Starting ConfigComponent")
    (let [cfg (config/load-config)]
      (log/info "ConfigComponent started successfully")
      (assoc this :value cfg)))
  (stop [this]
    (log/info "Stopping ConfigComponent")
    (dissoc this :value)))

(defn config-component []
  (map->ConfigComponent {}))

(defrecord DatabaseComponent [config]
  component/Lifecycle
  (start [this]
    (log/info "Starting DatabaseComponent")
    (let [cfg (:value config)
          spec (jdbc-spec-from-config cfg)]
      (log/info "Connecting to database" {:dbname (:dbname spec) :host (:host spec) :port (:port spec)})
      (try
        (let [datasource (jdbc/get-datasource spec)]
          (log/info "DatabaseComponent started successfully" {:dbname (:dbname spec)})
          (assoc this :datasource datasource))
        (catch Exception e
          (log/error e "Error connecting to database" {:dbname (:dbname spec) :host (:host spec)})
          (throw e)))))
  (stop [this]
    (log/info "Stopping DatabaseComponent")
    (dissoc this :datasource)))

(defn database-component []
  (map->DatabaseComponent {}))

(defrecord MigrationComponent [config database]
  component/Lifecycle
  (start [this]
    (log/info "Starting MigrationComponent")
    (let [cfg (:value config)
          uri (connection-uri-from-config cfg)
          migratus-config {:store :database
                           :migration-dir "migrations"
                           :db {:connection-uri uri}}
          start-time (System/currentTimeMillis)]
      (log/info "Running migrations")
      (try
        (migratus/migrate migratus-config)
        (let [duration (- (System/currentTimeMillis) start-time)]
          (log/info "Migrations completed successfully" {:duration-ms duration}))
        (catch Exception e
          (log/error e "Error running migrations")
          (throw e)))
      (assoc this :migratus-config migratus-config)))
  (stop [this]
    (log/info "Stopping MigrationComponent")
    this))

(defn migration-component []
  (map->MigrationComponent {}))

(defrecord RouterComponent [database]
  component/Lifecycle
  (start [this]
    (log/info "Starting RouterComponent")
    (try
      (let [ds (:datasource database)
            router (http-server/create-router ds)
            handler (http-server/create-handler router)]
        (log/info "RouterComponent started successfully")
        (assoc this :router router :handler handler))
      (catch Exception e
        (log/error e "Error creating router")
        (throw e))))
  (stop [this]
    (log/info "Stopping RouterComponent")
    (dissoc this :router :handler)))

(defn router-component []
  (map->RouterComponent {}))

(defrecord HttpServerComponent [config router]
  component/Lifecycle
  (start [this]
    (log/info "Starting HttpServerComponent")
    (let [cfg (:value config)
          port (http-port-from-config cfg)]
      (log/info "Starting HTTP server" {:port port})
      (try
        (let [handler (:handler router)
              server (jetty/run-jetty handler {:port port :join? false})]
          (log/info "HTTP server started successfully" {:port port})
          (assoc this :server server))
        (catch Exception e
          (log/error e "Error starting HTTP server" {:port port})
          (throw e)))))
  (stop [this]
    (log/info "Stopping HttpServerComponent")
    (when-let [server (:server this)]
      (log/info "Stopping HTTP server")
      (.stop server))
    (dissoc this :server)))

(defn http-server-component []
  (map->HttpServerComponent {}))

(defn new-system []
  (component/system-map
   :config (config-component)
   :database (component/using (database-component) [:config])
   :migrations (component/using (migration-component) [:config :database])
   :router (component/using (router-component) [:database])
   :http (component/using (http-server-component) [:config :router])))

