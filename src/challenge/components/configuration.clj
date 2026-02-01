(ns challenge.components.configuration
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [com.stuartsierra.component :as component]))

(defn- load-config-file
  [config-file logger]
  (try
    (let [resource (io/resource config-file)]
      (if resource
        (let [config (edn/read-string (slurp resource))]
          (when logger
            (.info logger (format "[Config] Configuration file loaded: %s" config-file)))
          config)
        (do
          (when logger
            (.error logger (format "[Config] Configuration file not found in classpath: %s" config-file)))
          (throw (ex-info (format "Configuration file not found: %s" config-file) {:config-file config-file})))))
    (catch Exception e
      (if logger
        (.error logger (format "[Config] Error loading configuration file: %s" config-file) e)
        (println "Error: Could not load config file" config-file ":" (.getMessage e)))
      (throw e))))

(defrecord ConfigComponent [config-file logger config]
  component/Lifecycle
  (start [this]
    (if config
      this
      (let [logger-instance (when logger (:logger logger))
            file-config (load-config-file config-file logger-instance)]
        (assoc this :config file-config))))
  (stop [this]
    (dissoc this :config)))

(defn new-config
  [config-file]
  (map->ConfigComponent {:config-file config-file}))

(defn get-config
  [config-component]
  (:config config-component))
