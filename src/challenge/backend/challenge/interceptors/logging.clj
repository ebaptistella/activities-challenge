(ns challenge.interceptors.logging
  (:require [challenge.components.logger :as logger]
            [challenge.interceptors.components :as interceptors.components]
            [cheshire.core :as json]
            [io.pedestal.interceptor :as interceptor]
            [schema.core :as s]))

(s/defn ^:private  sanitize-body
  "Sanitizes request/response body for logging (limits size, masks sensitive data).
   Handles various body types including InputStream, strings, maps, and other objects."
  [body max-size]
  (cond
    (nil? body) nil
    (string? body) (if (> (count body) max-size)
                     (str (subs body 0 max-size) "... [truncated]")
                     body)
    (map? body) (let [sanitized (dissoc body :password :token :secret)
                      json-str (json/generate-string sanitized)]
                  (if (> (count json-str) max-size)
                    (str (subs json-str 0 max-size) "... [truncated]")
                    sanitized))
    (instance? java.io.InputStream body) "[InputStream]"
    (instance? java.io.Reader body) "[Reader]"
    :else (let [body-str (str body)]
            (if (> (count body-str) max-size)
              (str (subs body-str 0 max-size) "... [truncated]")
              body-str))))

(s/defn ^:private  log-request
  "Logs incoming request details."
  [logger-comp request]
  (when logger-comp
    (let [method (:request-method request)
          uri (:uri request)
          path-params (:path-params request)
          query-params (:query-params request)
          headers (select-keys (:headers request) ["content-type" "authorization" "user-agent"])
          body (sanitize-body (or (:json-params request)
                                  (when-let [b (:body request)]
                                    (cond
                                      (string? b) b
                                      (instance? java.io.InputStream b) "[InputStream]"
                                      :else (str b))))
                              500)
          log (logger/bound logger-comp)]
      (logger/log-call log :info
                       "[Request] %s %s | Path-params: %s | Query-params: %s | Headers: %s | Body: %s"
                       (name method) uri path-params query-params headers body))))

(s/defn ^:private  log-response
  "Logs outgoing response details."
  [logger-comp response request]
  (when logger-comp
    (let [status (:status response)
          headers (select-keys (:headers response) ["content-type"])
          body (sanitize-body (:body response) 500)
          method (:request-method request)
          uri (:uri request)
          log (logger/bound logger-comp)]
      (logger/log-call log :info
                       "[Response] %s %s | Status: %s | Headers: %s | Body: %s"
                       (name method) uri status headers body))))

(def logging-interceptor
  "Interceptor to log incoming requests and outgoing responses."
  (interceptor/interceptor
   {:name ::logging
    :enter (fn [context]
             (let [request (:request context)
                   logger-comp (interceptors.components/get-component request :logger)]
               (log-request logger-comp request)
               context))
    :leave (fn [context]
             (let [request (:request context)
                   response (:response context)
                   logger-comp (interceptors.components/get-component request :logger)]
               (log-response logger-comp response request)
               context))}))
