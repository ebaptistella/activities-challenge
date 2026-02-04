(ns challenge.handlers.routes.health
  (:require [challenge.infrastructure.http-server.health :as http-server.health]))

(def routes
  #{["/health"
     :get
     http-server.health/health-check
     :route-name :health-check]})
