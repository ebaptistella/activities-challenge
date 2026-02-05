(ns challenge.infrastructure.http-server.health
  (:require [challenge.interface.http.response :as response]))

(defn health-check
  [_request]
  (response/ok {:status "ok"
                :service "challenge"}))
