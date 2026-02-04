(ns challenge.handlers.http-server
  (:require [challenge.handlers.routes.activity :as routes.activity]
            [challenge.handlers.routes.health :as routes.health]
            [challenge.handlers.routes.swagger :as routes.swagger]
            [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]))

(defn- combine-routes
  []
  (set (concat routes.health/routes
               routes.activity/routes
               routes.swagger/routes)))

(def routes
  (route/expand-routes (combine-routes)))

(def server-config
  (merge {::http/type :jetty
          ::http/routes routes}
         {::http/join? false}))