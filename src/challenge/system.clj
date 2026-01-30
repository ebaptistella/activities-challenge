(ns challenge.system
  (:require
   [challenge.config :as config]
   [com.stuartsierra.component :as component]
   [migratus.core :as migratus]
   [next.jdbc :as jdbc]
   [ring.adapter.jetty :as jetty]
   [volis-challenge.api :as api]
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

(defn- make-handler [ds]
  (api/handler ds))

(defrecord ConfigComponent []
  component/Lifecycle
  (start [this]
    (log/info "Iniciando ConfigComponent")
    (let [cfg (config/load-config)]
      (log/info "ConfigComponent iniciado com sucesso")
      (assoc this :value cfg)))
  (stop [this]
    (log/info "Parando ConfigComponent")
    (dissoc this :value)))

(defn config-component []
  (map->ConfigComponent {}))

(defrecord DatabaseComponent [config]
  component/Lifecycle
  (start [this]
    (log/info "Iniciando DatabaseComponent")
    (let [cfg (:value config)
          spec (jdbc-spec-from-config cfg)]
      (log/info "Conectando ao banco de dados" {:dbname (:dbname spec) :host (:host spec) :port (:port spec)})
      (try
        (let [datasource (jdbc/get-datasource spec)]
          (log/info "DatabaseComponent iniciado com sucesso" {:dbname (:dbname spec)})
          (assoc this :datasource datasource))
        (catch Exception e
          (log/error e "Erro ao conectar ao banco de dados" {:dbname (:dbname spec) :host (:host spec)})
          (throw e)))))
  (stop [this]
    (log/info "Parando DatabaseComponent")
    (dissoc this :datasource)))

(defn database-component []
  (map->DatabaseComponent {}))

(defrecord MigrationComponent [config database]
  component/Lifecycle
  (start [this]
    (log/info "Iniciando MigrationComponent")
    (let [cfg (:value config)
          uri (connection-uri-from-config cfg)
          migratus-config {:store :database
                           :migration-dir "migrations"
                           :db {:connection-uri uri}}
          start-time (System/currentTimeMillis)]
      (log/info "Executando migrations")
      (try
        (migratus/migrate migratus-config)
        (let [duration (- (System/currentTimeMillis) start-time)]
          (log/info "Migrations executadas com sucesso" {:duration-ms duration}))
        (catch Exception e
          (log/error e "Erro ao executar migrations")
          (throw e)))
      (assoc this :migratus-config migratus-config)))
  (stop [this]
    (log/info "Parando MigrationComponent")
    this))

(defn migration-component []
  (map->MigrationComponent {}))

(defrecord HttpServerComponent [config database]
  component/Lifecycle
  (start [this]
    (log/info "Iniciando HttpServerComponent")
    (let [cfg (:value config)
          port (http-port-from-config cfg)]
      (log/info "Iniciando servidor HTTP" {:port port})
      (try
        (let [ds (:datasource database)
              server (jetty/run-jetty (make-handler ds) {:port port :join? false})]
          (log/info "Servidor HTTP iniciado com sucesso" {:port port})
          (assoc this :server server))
        (catch Exception e
          (log/error e "Erro ao iniciar servidor HTTP" {:port port})
          (throw e)))))
  (stop [this]
    (log/info "Parando HttpServerComponent")
    (when-let [server (:server this)]
      (log/info "Parando servidor HTTP")
      (.stop server))
    (dissoc this :server)))

(defn http-server-component []
  (map->HttpServerComponent {}))

(defn new-system []
  (component/system-map
   :config (config-component)
   :database (component/using (database-component) [:config])
   :migrations (component/using (migration-component) [:config :database])
   :http (component/using (http-server-component) [:config :database :migrations])))

