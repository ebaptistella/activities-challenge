(ns challenge.main
  (:gen-class)
  (:require [challenge.system :as system]
            [com.stuartsierra.component :as component]
            [challenge.interceptors.components :as interceptors.components]))

(defn -main [& _args]
  (let [sys (component/start-system (system/new-dev-system))
        pedestal (:pedestal sys)
        logger (:logger sys)]
    (when logger
      (.info (:logger logger) "[System] System started successfully - all components are ready"))
    (try
      (when-let [jetty-server (:jetty-server pedestal)]
        (.join jetty-server))
      (catch InterruptedException _
        (component/stop-system sys)))))
