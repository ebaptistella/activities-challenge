(ns challenge.system
  (:require
   [challenge.config :as config]
   [com.stuartsierra.component :as component]
   [migratus.core :as migratus]
   [next.jdbc :as jdbc]
   [ring.adapter.jetty :as jetty]
   [volis-challenge.api :as api]))

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
    (assoc this :value (config/load-config)))
  (stop [this]
    (dissoc this :value)))

(defn config-component []
  (map->ConfigComponent {}))

(defrecord DatabaseComponent [config]
  component/Lifecycle
  (start [this]
    (let [cfg (:value config)
          spec (jdbc-spec-from-config cfg)
          datasource (jdbc/get-datasource spec)]
      (assoc this :datasource datasource)))
  (stop [this]
    (dissoc this :datasource)))

(defn database-component []
  (map->DatabaseComponent {}))

(defrecord MigrationComponent [config database]
  component/Lifecycle
  (start [this]
    (let [cfg (:value config)
          uri (connection-uri-from-config cfg)
          migratus-config {:store :database
                           :migration-dir "resources/migrations"
                           :db {:connection-uri uri}}
          ds (:datasource database)]
      (println "Executando migrations...")
      (println "URI de conexão:" uri)
      (try
        (migratus/migrate migratus-config)
        (println "Migrations executadas com sucesso")
        (let [tables (jdbc/execute! ds ["SELECT tablename FROM pg_tables WHERE schemaname = 'public' AND tablename = 'activity'"])]
          (if (seq tables)
            (println "Tabela 'activity' confirmada no banco de dados")
            (println "AVISO: Tabela 'activity' não encontrada após migrations!")))
        (catch Exception e
          (println "Erro ao executar migrations:" (.getMessage e))
          (.printStackTrace e)
          (throw e)))
      (assoc this :migratus-config migratus-config)))
  (stop [this]
    this))

(defn migration-component []
  (map->MigrationComponent {}))

(defrecord HttpServerComponent [config database]
  component/Lifecycle
  (start [this]
    (let [cfg (:value config)
          port (http-port-from-config cfg)
          ds (:datasource database)
          server (jetty/run-jetty (make-handler ds) {:port port :join? false})]
      (assoc this :server server)))
  (stop [this]
    (when-let [server (:server this)]
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

