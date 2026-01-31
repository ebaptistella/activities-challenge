(ns challenge.repl
  "REPL utilities with auto-start functionality."
  (:require [challenge.system :as system]
            [com.stuartsierra.component :as component]))

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

(defn reload!
  "Reload namespaces and restart the system. Useful for development when code changes."
  []
  (println "[System] Reloading namespaces...")
  (require 'challenge.handlers.http-server :reload)
  (require 'challenge.system :reload)
  (require 'challenge.components.pedestal :reload)
  (println "[System] Namespaces reloaded. Restarting system...")
  (restart!))

(defn auto-start!
  "Automatically starts the system when REPL is initialized."
  []
  (println "\n[REPL] Starting system...")
  (try
    (start!)
    (println "[REPL] ✓ System started successfully!")
    (catch Exception e
      (println (format "[REPL] ✗ Error: %s" (.getMessage e)))
      (println "[REPL] Start manually with: (challenge.repl/start!)"))))

;; Auto-start with delay when namespace is loaded
(future
  (Thread/sleep 1000)
  (when (nil? @system)
    (auto-start!)))

(comment
  (reload!))