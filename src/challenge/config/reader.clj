(ns challenge.config.reader
  (:require [challenge.components.configuration :as components.configuration]))

(def default-config-file
  "config/application.edn")

(def default-application-name
  "challenge")

(defn- get-config
  [config-component]
  (components.configuration/get-config config-component))

(defn- get-http-config
  [config-component]
  (get-in (get-config config-component) [:http]))

(defn http->port
  [config-component]
  (:port (get-http-config config-component)))

(defn http->host
  [config-component]
  (:host (get-http-config config-component)))

(defn config-file
  "Returns the config file path from the config component.
   Falls back to default-config-file if not found in config."
  [config-component]
  (or (:config-file (get-config config-component))
      default-config-file))

(defn application-name
  "Returns the application name from the config component.
   Falls back to default-application-name if not found in config."
  [config-component]
  (or (:application-name (get-config config-component))
      default-application-name))

(defn database-config
  "Returns the database config from the config component."
  [config-component]
  (get-in (get-config config-component) [:database]))