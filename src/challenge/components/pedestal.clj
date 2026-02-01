(ns challenge.components.pedestal
  (:require [challenge.config.reader :as config.reader]
            [challenge.interceptors.validation :as interceptors.validation]
            [com.stuartsierra.component :as component]
            [io.pedestal.http :as http]))

(defrecord PedestalComponent [server-config config logger server jetty-server system]
  component/Lifecycle
  (start [this]
    (if server
      this
      (let [base-config (or server-config {})
            final-config (if config
                           (let [port (config.reader/http->port config)]
                             (if port
                               (assoc base-config ::http/port port)
                               base-config))
                           base-config)
            config-with-context (assoc final-config
                                       ::http/context {:system this})
            config-with-interceptors (-> config-with-context
                                         http/default-interceptors
                                         http/dev-interceptors
                                         (update ::http/interceptors
                                                 (fn [interceptors]
                                                   (concat [interceptors.validation/json-body
                                                            interceptors.validation/json-response
                                                            interceptors.validation/error-handler-interceptor]
                                                           interceptors))))
            server-config-map (http/create-server config-with-interceptors)
            started-config (http/start server-config-map)
            jetty-instance (::http/server started-config)]
        (when logger
          (let [routes (::http/routes final-config)
                route-count (if (map? routes)
                              (count routes)
                              (if (sequential? routes)
                                (count routes)
                                (if (set? routes)
                                  (count routes)
                                  "N/A")))]
            (.info (:logger logger) (format "[Pedestal] Configuring routes: %s route(s) defined" route-count))
            (.info (:logger logger) (format "[Pedestal] Server started successfully on port %d" (::http/port final-config)))))
        (assoc this :server started-config :jetty-server jetty-instance :system this))))

  (stop [this]
    (when server
      (http/stop server)
      (when logger
        (.info (:logger logger) "[Pedestal] Server stopped successfully")))
    (dissoc this :server :jetty-server :system)))

(defn new-pedestal
  [server-config]
  (map->PedestalComponent {:server-config server-config}))
