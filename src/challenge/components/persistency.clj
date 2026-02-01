(ns challenge.components.persistency
  (:require [challenge.config.reader :as config.reader]
            [com.stuartsierra.component :as component]
            [next.jdbc.connection :as connection])
  (:import (com.zaxxer.hikari HikariDataSource)))

(defprotocol IPersistency
  "Protocol for database persistence operations"
  (get-datasource [this]
    "Returns the datasource for database operations"))

(defrecord PersistencyComponent [config logger datasource]
  component/Lifecycle
  (start [this]
    (if datasource
      this
      (let [connection-uri (config.reader/database-connection-uri-from-component config)
            ds (connection/->pool HikariDataSource {:jdbcUrl connection-uri})]
        (when logger
          (.info (:logger logger) "[Persistency] Database connection pool started"))
        (assoc this :datasource ds))))
  (stop [_this]
    (when datasource
      (.close datasource)
      (when logger
        (.info (:logger logger) "[Persistency] Database connection pool closed")))
    (dissoc _this :datasource))

  IPersistency
  (get-datasource [_this]
    datasource))

(defn new-persistency
  "Creates a new persistency component"
  []
  (map->PersistencyComponent {}))
