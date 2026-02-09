(ns challenge.interface.http.response
  (:require [schema.core :as s]))

(s/defn ok :- {:status s/Int :body s/Any}
  "200 OK response. Body can be any success payload (e.g. entity, {:items [...]})."
  [body]
  {:status 200 :body body})

(s/defn created :- {:status s/Int :body s/Any}
  "201 Created response. Body can be any created entity payload."
  [body]
  {:status 201 :body body})

(s/defn no-content :- {:status s/Int}
  "204 No Content response (no body)."
  []
  {:status 204})

(s/defn bad-request :- {:status s/Int :body {:error s/Str}}
  "400 Bad Request response"
  [error-message]
  {:status 400 :body {:error error-message}})

(s/defn not-found :- {:status s/Int :body {:error s/Str}}
  "404 Not Found response"
  [error-message]
  {:status 404 :body {:error error-message}})

(s/defn internal-server-error :- {:status s/Int :body {:error s/Str}}
  "500 Internal Server Error response"
  [error-message]
  {:status 500 :body {:error error-message}})
