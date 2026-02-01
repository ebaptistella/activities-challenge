(ns challenge.infrastructure.http-server.health
  (:require [cheshire.core :as json]))

(defn health-check
  [_request]
  {:status 200
   :headers {"Content-Type" "application/json"}
   :body (json/generate-string {:status "ok"
                                :service "challenge"})})
