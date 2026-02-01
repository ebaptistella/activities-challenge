(ns challenge.interceptors.components
  (:require [io.pedestal.http :as http.server]))

(defn- get-system
  "Gets the system from the request context."
  [request]
  (get-in request [::http.server/context :system]))

(defn get-component
  "Gets a component from the system by key.
   Returns the component or nil if not found."
  [request component-key]
  (get (get-system request) component-key))
