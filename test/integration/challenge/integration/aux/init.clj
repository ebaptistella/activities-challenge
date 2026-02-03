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

;; Atom to store the current mock persistency instance during test execution
(def ^:private mock-persistency-instance (atom nil))

;; Store original functions to restore them later
(def ^:private original-find-by-id (atom nil))
(def ^:private original-find-all (atom nil))
(def ^:private original-save! (atom nil))
(def ^:private original-delete! (atom nil))

(defn- setup-mocks!
  "Sets up mock functions for persistency operations"
  [mock-persistency]
  (reset! mock-persistency-instance mock-persistency)
  ;; Store original functions if not already stored
  (when (nil? @original-find-by-id)
    (reset! original-find-by-id persistency.activity/find-by-id)
    (reset! original-find-all persistency.activity/find-all)
    (reset! original-save! persistency.activity/save!)
    (reset! original-delete! persistency.activity/delete!))
  ;; Replace with mock implementations
  (alter-var-root #'persistency.activity/find-by-id
                  (constantly (fn [id _persistency]
                                (mock-persistency/find-by-id id mock-persistency))))
  (alter-var-root #'persistency.activity/find-all
                  (constantly (fn [_persistency]
                                (mock-persistency/find-all mock-persistency))))
  (alter-var-root #'persistency.activity/save!
                  (constantly (fn [activity _persistency]
                                (mock-persistency/save! activity mock-persistency))))
  (alter-var-root #'persistency.activity/delete!
                  (constantly (fn [id _persistency]
                                (mock-persistency/delete! id mock-persistency)))))

(defn- restore-originals!
  "Restores original persistency functions"
  []
  (when @original-find-by-id
    (alter-var-root #'persistency.activity/find-by-id (constantly @original-find-by-id))
    (alter-var-root #'persistency.activity/find-all (constantly @original-find-all))
    (alter-var-root #'persistency.activity/save! (constantly @original-save!))
    (alter-var-root #'persistency.activity/delete! (constantly @original-delete!))))

(defn init!
  "Initializes the test system with mocked components.
   Returns a function that returns the initial state for state-flow.
   This follows the pattern used in ordnungsamt project."
  []
  (fn []
    (let [server-config (assoc handlers.http-server/server-config
                               ::http/port 0  ; Use random port for tests
                               ::http/host "localhost")
          test-system (component/system-map
                       :logger (components.logger/new-logger "test")
                       :config (component/using
                                (components.configuration/new-config "config/application.edn")
                                [:logger])
                       :persistency (component/using
                                     (mock-persistency/new-mock-persistency)
                                     [])
                       :pedestal (component/using
                                  (components.pedestal/new-pedestal server-config)
                                  [:config :logger :persistency]))
          started-system (component/start-system test-system)
          ;; Extract components from started system for backward compatibility
          mock-persistency (:persistency started-system)
          logger (:logger started-system)
          config (:config started-system)
          pedestal (:pedestal started-system)]
      ;; Setup mock functions to use mock persistency
      (setup-mocks! mock-persistency)
      {:system started-system
       :persistency mock-persistency
       :logger logger
       :config config
       :pedestal pedestal})))

(defn cleanup!
  "Stops the test system and cleans up resources.
   Returns a function that takes state and cleans up."
  []
  (fn [state]
    (when-let [system (:system state)]
      (component/stop-system system))
    ;; Note: We don't restore original functions here because all tests need mocks
    ;; and tests run sequentially. Original functions are only needed if running
    ;; unit tests after integration tests in the same JVM session.
    ))

(defn- initialize-schema-validation!
  "Initializes the schema validation if schema.test is available.
   This function is idempotent and can be called multiple times without problems.
   
   Returns true if the validation was initialized successfully, false otherwise."
  []
  (try
    (when-not (find-ns 'schema.test)
      (require 'schema.test))
    (when-let [validate-fn (resolve 'schema.test/validate-schemas)]
      (validate-fn)
      true)
    (catch Exception _
      false)))

(initialize-schema-validation!)

(defmacro defflow
  "Defines a state-flow test with automatic system initialization and schema validation.
   
   Usage:
     (defflow my-test
       (flow \"test description\"
         (match? expected actual)))
   
   The system is automatically initialized before the test and stopped after.
   Persistency functions are automatically mocked to use in-memory storage.
   Schema validation is automatically enabled when the namespace is loaded."
  [name & body]
  `(do
     (try
       (when-not (find-ns 'schema.test)
         (require 'schema.test))
       (when-let [validate-fn# (resolve 'schema.test/validate-schemas)]
         (validate-fn#))
       (catch Exception _# nil))

     (flow/defflow ~name
       {:init (init!)
        :cleanup (cleanup!)
        :fail-fast? true}
       ~@body)))
