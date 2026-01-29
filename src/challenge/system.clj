(ns challenge.system
  (:require [challenge.config :as config]
            [com.stuartsierra.component :as component]
            [next.jdbc :as jdbc]
            [migratus.core :as migratus]
            [reitit.ring :as reitit.ring]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.multipart-params :as multipart]
            [ring.util.response :as response]
            [volis-challenge.csv :as csv]
            [volis-challenge.db :as db]
            [clojure.java.io :as io]))

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

(defn- import-summary [parsed]
  (let [{:keys [type rows errors]} parsed
        total (count rows)
        error-count (count errors)]
    {:type type
     :lines_read (+ total error-count)
     :valid total
     :invalid error-count
     :errors errors}))

(defn- import-handler [ds request]
  (let [file (get-in request [:params "file"])
        tempfile (:tempfile file)]
    (if (nil? tempfile)
      {:status 400
       :headers {}
       :body {:error "Arquivo CSV nao enviado"}}
      (try
        (with-open [r (io/reader tempfile)]
          (let [parsed (csv/parse-csv-reader r)
                {:keys [type rows]} parsed]
            (case type
              :planned (db/import-planned-batch! ds rows)
              :executed (db/import-executed-batch! ds rows))
            {:status 200
             :headers {}
             :body (import-summary parsed)}))
        (catch clojure.lang.ExceptionInfo e
          (let [data (ex-data e)]
            {:status 400
             :headers {}
             :body {:error (.getMessage e)
                    :details data}}))))))

(defn- make-handler [ds]
  (let [router (reitit.ring/router
                [["/health" {:get (fn [_] {:status 200 :headers {} :body "ok"})}]
                 ["/" {:get (fn [_] (response/resource-response "public/index.html"))}]
                 ["/api/import" {:post (fn [req] (import-handler ds req))}]])
        app (reitit.ring/ring-handler
             router
             (reitit.ring/create-resource-handler {:path "/"}))]
    (multipart/wrap-multipart-params app)))

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
                           :db {:connection-uri uri}}]
      (migratus/migrate migratus-config)
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
   :http (component/using (http-server-component) [:config :database])))

