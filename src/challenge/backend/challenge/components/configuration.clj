(ns challenge.components.configuration
  (:require [challenge.components.logger :as logger]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [com.stuartsierra.component :as component]))

(defn- load-config-file
  [config-file logger]
  (try
    (let [resource (io/resource config-file)]
      (if resource
        (let [config (edn/read-string (slurp resource))
              log (logger/bound logger)]
          (logger/log-call log :info "[Config] Configuration file loaded: %s" config-file)
          config)
        (do
          (let [log (logger/bound logger)]
            (logger/log-call log :error "[Config] Configuration file not found in classpath: %s" config-file))
          (throw (ex-info (format "Configuration file not found: %s" config-file) {:config-file config-file})))))
    (catch Exception e
      (if logger
        (let [log (logger/bound logger)]
          (logger/log-call log :error "[Config] Error loading configuration file: %s" config-file e))
        (println "Error: Could not load config file" config-file ":" (.getMessage e)))
      (throw e))))

(defrecord ConfigComponent [config-file logger config]
  component/Lifecycle
  (start [this]
    (if config
      this
      (let [file-config (load-config-file config-file logger)]
        (assoc this :config file-config))))
  (stop [this]
    (dissoc this :config)))

(defn new-config
  [config-file]
  (map->ConfigComponent {:config-file config-file}))

(defn get-config
  [config-component]
  (:config config-component))
