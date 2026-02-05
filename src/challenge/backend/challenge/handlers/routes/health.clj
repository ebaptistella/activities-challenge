(ns challenge.handlers.routes.health
  (:require [challenge.infrastructure.http-server.health :as http-server.health]
            [challenge.wire.out.health :as wire.out.health]))

(def routes
  #{["/health"
     :get
     http-server.health/health-check
     :route-name :health-check
     :summary "Health check endpoint"
     :doc "Checks if the service is running"
     :responses {200 {:body wire.out.health/HealthResponse
                      :description "Service is healthy"}}]})
