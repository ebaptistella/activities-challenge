(ns challenge.handlers.http-server
  (:require [challenge.handlers.routes.activity :as routes.activity]
            [challenge.handlers.routes.health :as routes.health]
            [challenge.handlers.routes.static :as routes.static]
            [challenge.handlers.routes.swagger :as routes.swagger]
            [challenge.infrastructure.http-server.swagger.doc :as swagger.doc]
            [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]))

(defn- combine-routes
  []
  (set (concat routes.health/routes
               routes.activity/routes
               routes.static/routes)))

(def all-routes-with-docs
  (let [api-routes (combine-routes)
        swagger-routes (routes.swagger/create-swagger-routes api-routes)]
    (concat api-routes swagger-routes)))

(def routes
  (route/expand-routes (swagger.doc/clean-routes-for-pedestal all-routes-with-docs)))

(def server-config
  (merge {::http/type :jetty
          ::http/routes routes
          ::http/resource-path "/public"}
         {::http/join? false}))