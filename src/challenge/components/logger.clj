(ns challenge.components.logger
  (:require [com.stuartsierra.component :as component])
  (:import (org.slf4j LoggerFactory)))

(defrecord LoggerComponent [logger-name logger]
  component/Lifecycle
  (start [this]
    (if logger
      this
      (let [name (or logger-name "challenge")
            logger-instance (LoggerFactory/getLogger name)]
        (assoc this :logger logger-instance))))
  (stop [this]
    (dissoc this :logger)))

(defn new-logger
  ([]
   (new-logger "challenge"))
  ([logger-name]
   (map->LoggerComponent {:logger-name logger-name})))