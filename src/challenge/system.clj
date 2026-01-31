(ns challenge.system
  (:require [com.stuartsierra.component :as component]
            [challenge.components.configuration :as components.configuration]
            [challenge.components.logger :as components.logger]
            [challenge.components.pedestal :as components.pedestal]
            [challenge.handlers.http-server :as handlers.http-server]))

(defn new-system
  ([]
   (new-system {}))
  ([{:keys [server-config logger-name]
     :or {server-config handlers.http-server/server-config
          logger-name "challenge"}}]
   (component/system-map
    :logger (components.logger/new-logger logger-name)
    :config (component/using
             (components.configuration/new-config "config/application.edn")
             [:logger])
    :pedestal (component/using
               (components.pedestal/new-pedestal server-config)
               [:config :logger]))))

(defn new-dev-system
  []
  (new-system {:server-config handlers.http-server/server-config}))