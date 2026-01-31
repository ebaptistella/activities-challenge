(ns challenge.handlers.web
  (:require [io.pedestal.http :as pedestal.http]
            [io.pedestal.http.body-params :as pedestal.http.body-params]
            [io.pedestal.http.route :as pedestal.http.route]
            [challenge.interceptors.components :as interceptors.components]))

(def common-interceptors
  [(pedestal.http.body-params/body-params)
   (interceptors.components/inject-system)
   (interceptors.components/inject-logger)])

(defn routes []
  [#_#{["/" :get (conj common-interceptors home) :route-name :home]}])

(def server-config
  {::pedestal.http/port 8080
   ::pedestal.http/type :jetty
   ::pedestal.http/resource-path "/public"
   ::pedestal.http/routes (pedestal.http.route/expand-routes (routes))})

(def dev-server-config
  (merge server-config
         {:env :dev
          ::pedestal.http/join? false
          ::pedestal.http/routes #(pedestal.http.route/expand-routes (routes))
          ::pedestal.http/allowed-origins {:creds true :allowed-origins (constantly true)}
          ::pedestal.http/secure-headers {:content-security-policy-settings {:object-src "'none'"}}}))