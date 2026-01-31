(ns challenge.main
  (:gen-class)
  (:require [challenge.system :as system]
            [com.stuartsierra.component :as component]
            [challenge.interceptors.components :as interceptors.components]))

(defonce system (atom nil))

(defn start!
  "Start the system. Useful for REPL development."
  []
  (if @system
    (do
      (println "[System] System already started. Call stop! first.")
      @system)
    (let [_ (reset! system (component/start-system (system/new-dev-system)))
          pedestal (:pedestal @system)
          logger (:logger @system)]
      (when logger
        (.info (:logger logger) "[System] System started successfully - all components are ready"))
      (println "[System] System started successfully - all components are ready")
      (when pedestal
        (println "[System] Server running. Check logs for port information."))
      @system)))

(defn stop!
  "Stop the system. Useful for REPL development."
  []
  (when @system
    (component/stop-system @system)
    (reset! system nil)
    (println "[System] System stopped successfully"))
  (when (nil? @system)
    (println "[System] System was not running")))

(defn restart!
  "Restart the system. Useful for REPL development."
  []
  (stop!)
  (start!))

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
