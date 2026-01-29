(ns challenge.core
  (:gen-class)
  (:require [com.stuartsierra.component :as component]
            [challenge.system :as system]))

(defonce app-system (atom nil))

(defn start []
  (let [s (system/new-system)]
    (reset! app-system (component/start s))))

(defn stop []
  (when-let [s @app-system]
    (component/stop s)
    (reset! app-system nil)))

(defn -main
  [& _]
  (start)
  (.addShutdownHook
   (Runtime/getRuntime)
   (Thread. ^Runnable #(stop))))
