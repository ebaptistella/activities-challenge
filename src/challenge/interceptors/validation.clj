(ns challenge.interceptors.validation
  (:require [cheshire.core :as json]
            [io.pedestal.interceptor :as interceptor]))

(def json-body
  "Interceptor to parse JSON request body and add it to :json-params key.
   Uses Cheshire for JSON parsing."
  (interceptor/interceptor
   {:name ::json-body
    :enter (fn [context]
             (let [request (:request context)
                   body (:body request)]
               (if (and body (string? body) (not (empty? body)))
                 (try
                   (let [parsed-body (json/parse-string body true)]
                     (assoc-in context [:request :json-params] parsed-body))
                   (catch Exception _
                     (assoc context
                            :response {:status 400
                                       :headers {"Content-Type" "application/json"}
                                       :body (json/generate-string {:error "Invalid JSON"})})))
                 context)))}))

(def json-response
  "Interceptor to serialize response body to JSON if it's a map or collection.
   Uses Cheshire for JSON serialization."
  (interceptor/interceptor
   {:name ::json-response
    :leave (fn [context]
             (let [response (:response context)
                   body (:body response)]
               (if (or (map? body) (sequential? body))
                 (assoc-in context [:response :body] (json/generate-string body))
                 context)))}))

(def error-handler-interceptor
  "Interceptor to handle errors and return appropriate JSON responses."
  (interceptor/interceptor
   {:name ::error-handler
    :error (fn [context _exception]
             (let [response {:status 500
                             :headers {"Content-Type" "application/json"}
                             :body (json/generate-string {:error "Internal server error"})}]
               (assoc context :response response)))}))
