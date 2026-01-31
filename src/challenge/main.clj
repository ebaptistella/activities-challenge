(ns challenge.main
  (:gen-class)
  (:require [challenge.system :as system]
            [com.stuartsierra.component :as component]
            [challenge.interceptors.components :as interceptors.components]))

(defn -main [& _args]
  (println "\nStarting system with Component...")
  (let [sys (component/start-system (system/new-dev-system))
        pedestal (interceptors.components/get-component sys :pedestal)
        logger (interceptors.components/get-component sys :logger)]
    (when logger
      (.info (:logger logger) "System started successfully"))
    (println "\nServer running. Press Ctrl+C to stop.")

    (when pedestal
      (.join (:server pedestal)))))