(ns challenge.system
  (:require [challenge.components.configuration :as components.configuration]
            [challenge.components.logger :as components.logger]
            [challenge.components.migration :as components.migration]
            [challenge.components.pedestal :as components.pedestal]
            [challenge.components.persistency :as components.persistency]
            [challenge.config.reader :as config.reader]
            [challenge.handlers.http-server :as handlers.http-server]
            [com.stuartsierra.component :as component]))

(defn new-system
  ([]
   (new-system {}))
  ([{:keys [server-config logger-name]
     :or {server-config handlers.http-server/server-config
          logger-name config.reader/default-application-name}}]
   (component/system-map
    :logger (components.logger/new-logger logger-name)
    :config (component/using
             (components.configuration/new-config config.reader/default-config-file)
             [:logger])
    :migration (component/using
                (components.migration/new-migration)
                [:config :logger])
    :persistency (component/using
                  (components.persistency/new-persistency)
                  [:config :logger])
    :pedestal (component/using
               (components.pedestal/new-pedestal server-config)
               [:config :logger]))))

(defn new-dev-system
  []
  (new-system {:server-config handlers.http-server/server-config}))