(ns challenge.system
  (:require
   [challenge.config :as config]
   [clojure.java.io :as io]
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
      (println "Conectando ao banco de dados:" (:dbname spec) "em" (:host spec))
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
          migration-resource (io/resource "migrations")
          migration-dir-path (if migration-resource
                               (let [url-str (str migration-resource)]
                                 (if (.startsWith url-str "file:")
                                   (let [file-path (.substring url-str 5)
                                         file (io/file file-path)]
                                     (if (.exists file)
                                       (.getAbsolutePath file)
                                       file-path))
                                   (if (.startsWith url-str "jar:")
                                     "migrations"
                                     "resources/migrations")))
                               "resources/migrations")
          migratus-config {:store :database
                           :migration-dir migration-dir-path
                           :db {:connection-uri uri}}]
      (println "Executando migrations...")
      (println "Diretório de migrations:" migration-dir-path)
      (println "URI de conexão:" uri)
      (try
        (let [pending (migratus/pending-list migratus-config)]
          (println "Migrations pendentes:" (count pending))
          (doseq [m pending]
            (println "  -" (:name m))))
        (migratus/migrate migratus-config)
        (println "Migrations executadas com sucesso")
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

