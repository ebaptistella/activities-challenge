(ns challenge.interface.http.response
  (:require [schema.core :as s]))

(s/defn ok
  "200 OK response"
  [body]
  {:status 200 :body body})

(s/defn created
  "201 Created response"
  [body]
  {:status 201 :body body})

(s/defn no-content
  "204 No Content response"
  []
  {:status 204})

(s/defn bad-request
  "400 Bad Request response"
  [error-message]
  {:status 400 :body {:error error-message}})

(s/defn not-found
  "404 Not Found response"
  [error-message]
  {:status 404 :body {:error error-message}})

(s/defn internal-server-error
  "500 Internal Server Error response"
  [error-message]
  {:status 500 :body {:error error-message}})
