(ns challenge.main
  (:gen-class)
  (:require [challenge.components.logger :as logger]
            [challenge.system :as system]
            [com.stuartsierra.component :as component]))

(defn -main [& _args]
  (let [sys (component/start-system (system/new-dev-system))
        pedestal (:pedestal sys)
        log (logger/bound (:logger sys))]
    (logger/log-call log :info "[System] System started successfully - all components are ready")
    (try
      (when-let [jetty-server (:jetty-server pedestal)]
        (.join jetty-server))
      (catch InterruptedException _
        (component/stop-system sys)))))
