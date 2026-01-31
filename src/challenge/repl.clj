(ns challenge.repl
  "REPL utilities with auto-start functionality."
  (:require [challenge.main :as main]))

(defn auto-start!
  "Automatically starts the system when REPL is initialized."
  []
  (println "\n[REPL] Starting system...")
  (try
    (main/start!)
    (println "[REPL] ✓ System started successfully!")
    (catch Exception e
      (println (format "[REPL] ✗ Error: %s" (.getMessage e)))
      (println "[REPL] Start manually with: (main/start!)"))))

;; Auto-start with delay when namespace is loaded
(future
  (Thread/sleep 1000)
  (when (nil? @main/system)
    (auto-start!)))
