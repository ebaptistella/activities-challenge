(ns challenge.interceptors.components
  (:require [io.pedestal.http :as http.server]))

(defn- get-system
  [request]
  (get-in request [::http.server/context :system]))

(defn get-component
  [request component-key]
  (get (get-system request) component-key))
