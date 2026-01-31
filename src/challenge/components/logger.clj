(ns challenge.components.logger
  (:require [challenge.config.reader :as config.reader]
            [com.stuartsierra.component :as component])
  (:import (org.slf4j LoggerFactory)))

(defrecord LoggerComponent [logger-name logger]
  component/Lifecycle
  (start [this]
    (if logger
      this
      (let [logger-name-str (or logger-name config.reader/default-application-name)
            logger-instance (LoggerFactory/getLogger logger-name-str)]
        (assoc this :logger logger-instance))))
  (stop [this]
    (dissoc this :logger)))

(defn new-logger
  ([]
   (new-logger config.reader/default-application-name))
  ([logger-name]
   (map->LoggerComponent {:logger-name logger-name})))