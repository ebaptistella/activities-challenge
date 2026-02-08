(ns challenge.components.logger
  (:require [com.stuartsierra.component :as component]
            [schema.core :as s])
  (:import (org.slf4j LoggerFactory)))

(def default-logger-name
  "Default logger name used when no logger-name is provided."
  "app")

(defprotocol ILogger
  "Protocol for logging operations. Similar to IPersistency, allows typed logger parameters.
   
   All methods support optional formatting via args vector:
   - (info logger \"message\" []) - simple message
   - (info logger \"format %s\" [arg]) - formatted message
   - (error logger \"message\" [] exception) - error with throwable
   - (error logger \"format %s\" [arg] exception) - formatted error with throwable"
  (info [this message args]
    "Logs an info message. If args are provided, treats message as format string.")
  (debug [this message args]
    "Logs a debug message. If args are provided, treats message as format string.")
  (warn [this message args]
    "Logs a warning message. If args are provided, treats message as format string.")
  (error [this message args throwable]
    "Logs an error message. If args are provided, treats message as format string.
     Throwable is optional and passed to the underlying logger if provided."))

(defrecord LoggerComponent [logger-name logger]
  component/Lifecycle
  (start [this]
    (if logger
      this
      (let [logger-name-str (or logger-name default-logger-name)
            logger-instance (LoggerFactory/getLogger logger-name-str)]
        (assoc this :logger logger-instance))))
  (stop [this]
    (dissoc this :logger))

  ILogger
  (info [this message args]
    (when-let [logger-instance logger]
      (if (seq args)
        (.info logger-instance (apply format message args))
        (.info logger-instance message))))
  (debug [this message args]
    (when-let [logger-instance logger]
      (if (seq args)
        (.debug logger-instance (apply format message args))
        (.debug logger-instance message))))
  (warn [this message args]
    (when-let [logger-instance logger]
      (if (seq args)
        (.warn logger-instance (apply format message args))
        (.warn logger-instance message))))
  (error [this message args throwable]
    (when-let [logger-instance logger]
      (let [final-message (if (seq args)
                            (apply format message args)
                            message)]
        (if throwable
          (.error logger-instance final-message throwable)
          (.error logger-instance final-message))))))

;; Helper functions that call the protocol methods with varargs support
;; These allow using varargs and provide a clean API
;; Protocol methods require args as vector, so these helpers convert varargs to vector
;; Note: Protocol methods (info, debug, warn, error) are available as functions,
;; but they don't support varargs. These helpers wrap them to add varargs support.

;; Note: Protocol methods (info, debug, warn, error) are available as functions automatically.
;; They accept (logger message args) or (logger message args throwable) for error.
;; To use varargs, call them directly: (info logger "message" [arg1 arg2])
;; Or use the bound/log-call helpers which handle varargs conversion.

(s/defn bound
  "Returns a map of logging functions with the logger-component already bound.
   Use with log-call for cleaner syntax.
   
   Usage:
     (let [log (logger/bound logger-component)]
       (log-call log :info \"message\")
       (log-call log :info \"format %s\" arg)
       (log-call log :error \"Error: %s\" msg exception))"
  [logger-component]
  {:info (fn [message & args]
           (when logger-component
             (info logger-component message args)))
   :debug (fn [message & args]
            (when logger-component
              (debug logger-component message args)))
   :warn (fn [message & args]
           (when logger-component
             (warn logger-component message args)))
   :error (fn [message & args]
            (when logger-component
              (let [has-throwable? (and (seq args) (instance? Throwable (last args)))
                    [format-args throwable] (if has-throwable?
                                              [(butlast args) (last args)]
                                              [args nil])]
                (error logger-component message format-args throwable))))})

(s/defn log-call
  "Calls a logging function from a bound logger.
   
   Usage:
     (let [log (logger/bound logger-component)]
       (log-call log :info \"message\")
       (log-call log :info \"format %s\" arg)
       (log-call log :error \"Error: %s\" msg exception))
   
   Log levels: :info, :debug, :warn, :error
   All support optional formatting via varargs."
  [bound-logger log-level & args]
  (when-let [log-fn (get bound-logger log-level)]
    (apply log-fn args)))

(s/defn new-logger
  ([]
   (new-logger default-logger-name))
  ([logger-name]
   (map->LoggerComponent {:logger-name logger-name})))