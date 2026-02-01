(ns challenge.infrastructure.http-server.health
  (:require [io.pedestal.interceptor :as interceptor]))

(def health-check
  "Health check endpoint interceptor."
  (interceptor/interceptor
   {:name ::health-check
    :enter (fn [context]
             (assoc context
                    :response {:status 200
                               :headers {"Content-Type" "application/json"}
                               :body "{\"status\":\"ok\",\"service\":\"challenge\"}"}))}))
