(ns challenge.interceptors.components
  (:require [io.pedestal.interceptor :as pedestal.interceptor]
            [io.pedestal.http :as http.server]))

(defn inject-system
  []
  (pedestal.interceptor/interceptor
   {:name ::inject-system
    :enter (fn [context]
             (let [system (get-in context [::http.server/context :system])]
               (assoc context :system system)))}))

(defn inject-logger
  []
  (pedestal.interceptor/interceptor
   {:name ::inject-logger
    :enter (fn [context]
             (let [system (:system context)
                   logger (get-in system [:logger :logger])]
               (assoc context :logger logger)))}))

(defn inject-component
  [component-key]
  (pedestal.interceptor/interceptor
   {:name (keyword "inject" (name component-key))
    :enter (fn [context]
             (let [system (:system context)
                   component (get system component-key)]
               (assoc context component-key component)))}))

(defn get-system
  [request]
  (get-in request [::http.server/context :system]))

(defn get-component
  [request component-key]
  (get (get-system request) component-key))
