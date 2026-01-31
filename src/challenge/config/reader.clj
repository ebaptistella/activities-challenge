(ns challenge.config.reader
  (:require [challenge.components.configuration :as components.configuration]))

(defn- get-config
  [config-component]
  (components.configuration/get-config config-component))

(defn- get-http-config
  [config-component]
  (get-in (get-config config-component) [:http]))

(defn http->port
  [config-component]
  (:port (get-http-config config-component)))
