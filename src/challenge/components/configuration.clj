(ns challenge.components.configuration
  (:require [com.stuartsierra.component :as component]
            [clojure.java.io :as io]
            [clojure.edn :as edn]))

(defn- load-config-file
  [config-file logger]
  (try
    (let [resource (io/resource config-file)
          file (when (not resource) (io/file config-file))
          source (or resource file)]
      (if source
        (let [config (edn/read-string (slurp source))]
          (when logger
            (.info logger (str "Config file loaded: " config-file)))
          config)
        (do
          (when logger
            (.warn logger (str "Config file not found: " config-file)))
          {})))
    (catch Exception e
      (if logger
        (.error logger (str "Could not load config file " config-file) e)
        (println "Warning: Could not load config file" config-file ":" (.getMessage e)))
      {})))

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
