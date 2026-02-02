(ns challenge.integration.aux.init
  (:require [challenge.components.configuration :as components.configuration]
            [challenge.components.logger :as components.logger]
            [challenge.components.pedestal :as components.pedestal]
            [challenge.handlers.http-server :as handlers.http-server]
            [challenge.infrastructure.persistency.activity :as persistency.activity]
            [challenge.integration.aux.mock-persistency :as mock-persistency]
            [com.stuartsierra.component :as component]
            [io.pedestal.http :as http]
            [state-flow.api :as flow]))

(defn init!
  "Initializes the test system with mocked components.
   Returns a function that returns the initial state for state-flow.
   This follows the pattern used in ordnungsamt project."
  []
  (fn []
    (let [mock-persistency (component/start (mock-persistency/new-mock-persistency))
          logger (component/start (components.logger/new-logger "test"))
          config (component/start (components.configuration/new-config "config/application.edn"))
          server-config (assoc handlers.http-server/server-config
                               ::http/port 0  ; Use random port for tests
                               ::http/host "localhost")
          pedestal (component/start (components.pedestal/new-pedestal server-config))
          test-system (component/system-map
                       :logger logger
                       :config config
                       :persistency mock-persistency
                       :pedestal pedestal)]
      ;; Mock persistency functions to use mock implementation
      (clojure.core/with-redefs [persistency.activity/find-by-id (fn [id persistency]
                                                                   (mock-persistency/find-by-id id mock-persistency))
                                 persistency.activity/find-all (fn [persistency]
                                                                 (mock-persistency/find-all mock-persistency))
                                 persistency.activity/save! (fn [activity persistency]
                                                              (mock-persistency/save! activity mock-persistency))
                                 persistency.activity/delete! (fn [id persistency]
                                                                (mock-persistency/delete! id mock-persistency))]
        {:system test-system
         :persistency mock-persistency
         :logger logger
         :config config
         :pedestal pedestal}))))

(defn cleanup!
  "Stops the test system and cleans up resources.
   Returns a function that takes state and cleans up."
  []
  (fn [state]
    (when-let [system (:system state)]
      (component/stop-system system))))

(defmacro defflow
  "Defines a state-flow test with automatic system initialization and schema validation.
   
   Usage:
     (defflow my-test
       (flow \"test description\"
         (match? expected actual)))
   
   The system is automatically initialized before the test and stopped after.
   Persistency functions are automatically mocked to use in-memory storage.
   
   This follows the pattern from ordnungsamt project:
   https://github.com/nubank/ordnungsamt/blob/main/test/integration/integration/aux/init.clj"
  [name & body]
  `(flow/defflow ~name
     {:init (init!)
      :cleanup (cleanup!)
      :fail-fast? true}
     ~@body))
