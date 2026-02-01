(ns challenge.components.pedestal
  (:require [challenge.config.reader :as config.reader]
            [challenge.interceptors.logging :as interceptors.logging]
            [challenge.interceptors.validation :as interceptors.validation]
            [com.stuartsierra.component :as component]
            [io.pedestal.http :as http]
            [io.pedestal.interceptor :as interceptor]))

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
            ;; Create an interceptor to inject context into request
            ;; This ensures the context is available in the request for other interceptors
            context-interceptor (interceptor/interceptor
                                 {:name ::inject-context
                                  :enter (fn [context]
                                           (let [context-map (::http/context config-with-context)]
                                             ;; Use http.server namespace to match interceptors.components
                                             (assoc-in context [:request ::http/context] context-map)))})
            config-with-interceptors (-> config-with-context
                                         http/default-interceptors
                                         http/dev-interceptors
                                         (update ::http/interceptors
                                                 (fn [interceptors]
                                                   ;; Add context injection interceptor first, then logging
                                                   (concat interceptors
                                                           [context-interceptor
                                                            interceptors.logging/logging-interceptor
                                                            interceptors.validation/json-body
                                                            interceptors.validation/json-response
                                                            interceptors.validation/error-handler-interceptor]))))
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
