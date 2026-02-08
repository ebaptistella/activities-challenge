(ns challenge.components.pedestal
  (:require [challenge.components.logger :as logger]
            [challenge.config.reader :as config.reader]
            [challenge.interceptors.logging :as interceptors.logging]
            [challenge.interceptors.validation :as interceptors.validation]
            [com.stuartsierra.component :as component]
            [io.pedestal.http :as http]
            [io.pedestal.interceptor :as interceptor]
            [schema.core :as s]))

(defrecord PedestalComponent [server-config config logger persistency server jetty-server system]
  component/Lifecycle
  (start [this]
    (if server
      this
      (let [base-config (or server-config {})
            ;; Preserve port 0 (random port for tests) if already set
            base-port (::http/port base-config)
            final-config (if config
                           (let [config-port (config.reader/http->port config)
                                 config-host (config.reader/http->host config)
                                 ;; Only override port if base-config doesn't have port 0 (random port)
                                 ;; Port 0 is used in tests to get a random available port
                                 config-with-port (if (and config-port (not= 0 base-port))
                                                    (assoc base-config ::http/port config-port)
                                                    base-config)]
                             (if config-host
                               (assoc config-with-port ::http/host config-host)
                               config-with-port))
                           base-config)
            ;; Build the full system map from available components
            ;; In Component System, components have access to their dependencies
            ;; We construct a system map with all components for the interceptor
            full-system-map {:logger logger
                             :config config
                             :persistency persistency
                             :pedestal this}
            config-with-context (assoc final-config
                                       ::http/context {:system full-system-map})
            ;; Create an interceptor to inject context and components into request
            ;; Components are injected under :components key for easy destructuring
            ;; Capture full-system-map in closure to avoid accessing config-with-context
            context-interceptor (let [captured-system full-system-map]
                                  (interceptor/interceptor
                                   {:name ::inject-context
                                    :enter (fn [context]
                                             (let [request (:request context)
                                                   ;; Get system from request context (injected by Pedestal from config)
                                                   ;; Fallback to captured system if not in request
                                                   system-from-request (or (get-in request [::http/context :system])
                                                                           captured-system)
                                                   ;; Get components from the system
                                                   persistency-comp (:persistency system-from-request)
                                                   logger-comp (:logger system-from-request)
                                                   config-comp (:config system-from-request)
                                                   pedestal-comp (:pedestal system-from-request)]
                                               (logger/log-call (logger/bound logger-comp)
                                                                :debug
                                                                "[Pedestal Interceptor] Injecting components | persistency: %s | logger: %s | config: %s"
                                                                (some? persistency-comp)
                                                                (some? logger-comp)
                                                                (some? config-comp))
                                               (-> context
                                                   ;; Inject full context for backward compatibility
                                                   (assoc-in [:request ::http/context] {:system system-from-request})
                                                   ;; Inject components under :components key
                                                   (assoc-in [:request :components] {:persistency persistency-comp
                                                                                     :logger logger-comp
                                                                                     :config config-comp
                                                                                     :pedestal pedestal-comp}))))}))
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
            server-config-map (try
                                (http/create-server config-with-interceptors)
                                (catch Exception e
                                  (let [log (logger/bound logger)]
                                    (logger/log-call log :error
                                                     "[Pedestal] Error creating server: %s | Exception: %s | Stack: %s"
                                                     (.getMessage e)
                                                     (pr-str e)
                                                     (pr-str (take 10 (map str (.getStackTrace e)))))
                                    (throw e))))
            started-config (try
                             (http/start server-config-map)
                             (catch Exception e
                               (let [log (logger/bound logger)]
                                 (logger/log-call log :error
                                                  "[Pedestal] Error starting server: %s | Exception: %s | Stack: %s"
                                                  (.getMessage e)
                                                  (pr-str e)
                                                  (pr-str (take 10 (map str (.getStackTrace e)))))
                                 (throw e))))
            jetty-instance (::http/server started-config)]
        (let [routes (::http/routes final-config)
              route-count (if (map? routes)
                            (count routes)
                            (if (sequential? routes)
                              (count routes)
                              (if (set? routes)
                                (count routes)
                                "N/A")))
              log (logger/bound logger)]
          (logger/log-call log :info "[Pedestal] Configuring routes: %s route(s) defined" route-count)
          (logger/log-call log :info "[Pedestal] Server started successfully on %s:%d"
                           (::http/host final-config)
                           (::http/port final-config)))
        (assoc this :server started-config :jetty-server jetty-instance :system full-system-map))))

  (stop [this]
    (when server
      (http/stop server)
      (let [log (logger/bound logger)]
        (logger/log-call log :info "[Pedestal] Server stopped successfully")))
    (dissoc this :server :jetty-server :system)))

(s/defn new-pedestal
  [server-config]
  (map->PedestalComponent {:server-config server-config}))
