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
  "Interceptor that automatically serializes response bodies to JSON and sets Content-Type header.
   Handles maps, collections, nil bodies, and already-serialized strings.
   If body is already a string, assumes it's already JSON."
  (interceptor/interceptor
   {:name ::json-response
    :leave (fn [context]
             (let [response (:response context)]
               (if-not response
                 context
                 (let [body (:body response)
                       status (:status response)
                       headers (or (:headers response) {})
                       content-type (get headers "Content-Type")
                       ;; Determine the serialized body based on type
                       serialized-body (cond
                                         ;; Handle 204 No Content (no body)
                                         (and (nil? body) (= status 204))
                                         nil

                                         ;; Skip if already JSON string
                                         (string? body)
                                         body

                                         ;; Serialize Clojure data structures (maps, vectors, lists, sets)
                                         (or (map? body) (sequential? body) (set? body))
                                         (json/generate-string body)

                                         ;; Handle other types (wrap in object)
                                         (some? body)
                                         (json/generate-string {:value (str body)})

                                         ;; Default: nil
                                         :else
                                         nil)
                       ;; Build updated response
                       updated-response (cond-> response
                                          ;; Set Content-Type header if not present
                                          (not content-type)
                                          (assoc-in [:headers "Content-Type"] "application/json")

                                          ;; Update body
                                          (some? serialized-body)
                                          (assoc :body serialized-body)

                                          ;; Remove body for 204
                                          (nil? serialized-body)
                                          (dissoc :body))]
                   (assoc context :response updated-response)))))}))

(def error-handler-interceptor
  "Interceptor to handle errors and return appropriate JSON responses.
   The json-response interceptor will automatically serialize the body."
  (interceptor/interceptor
   {:name ::error-handler
    :error (fn [context _exception]
             (assoc context :response {:status 500
                                       :body {:error "Internal server error"}}))}))
