(ns challenge.components.pedestal
  (:require [com.stuartsierra.component :as component]
            [io.pedestal.http :as pedestal.http]
            [challenge.config.config :as app-config]))

(defrecord PedestalComponent [server-config config logger server system]
  component/Lifecycle
  (start [this]
    (if server
      this
      (let [base-config (or server-config {})
            final-config (if config
                           (let [port (app-config/http->port config)]
                             (if port
                               (assoc base-config ::pedestal.http/port port)
                               base-config))
                           base-config)
            config-with-context (assoc final-config
                                       ::pedestal.http/context {:system this})
            server-instance (-> config-with-context
                                pedestal.http/default-interceptors
                                pedestal.http/dev-interceptors
                                pedestal.http/create-server
                                pedestal.http/start)]
        (when logger
          (.info (:logger logger) (str "Pedestal server started on port " (::pedestal.http/port config-with-context))))
        (assoc this :server server-instance :system this))))

  (stop [this]
    (when server
      (pedestal.http/stop server)
      (when logger
        (.info (:logger logger) "Pedestal server stopped")))
    (dissoc this :server :system)))

(defn new-pedestal
  [server-config]
  (map->PedestalComponent {:server-config server-config}))
