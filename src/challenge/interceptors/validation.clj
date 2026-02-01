(ns challenge.interceptors.validation
  (:require [challenge.interface.http.response :as response]
            [challenge.interceptors.components :as interceptors.components]
            [cheshire.core :as json]
            [io.pedestal.interceptor :as interceptor]
            [schema.core :as s]))

(def json-body
  "Interceptor to parse JSON request body and add it to :json-params key.
   Uses Cheshire for JSON parsing."
  (interceptor/interceptor
   {:name ::json-body
    :enter (fn [context]
             (let [request (:request context)
                   body (:body request)
                   logger-comp (interceptors.components/get-component request :logger)
                   logger (when logger-comp (:logger logger-comp))]
               (if (and body (string? body) (not (empty? body)))
                 (try
                   (let [parsed-body (json/parse-string body true)]
                     (when logger
                       (.debug logger (format "[JSON Body] Successfully parsed JSON body for %s %s"
                                              (name (:request-method request))
                                              (:uri request))))
                     (assoc-in context [:request :json-params] parsed-body))
                   (catch Exception e
                     (when logger
                       (.warn logger (format "[JSON Body] Failed to parse JSON body for %s %s: %s"
                                             (name (:request-method request))
                                             (:uri request)
                                             (.getMessage e))))
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
             (let [request (:request context)
                   response (:response context)
                   logger-comp (interceptors.components/get-component request :logger)
                   logger (when logger-comp (:logger logger-comp))]
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
                   (when logger
                     (.debug logger (format "[JSON Response] Serialized response body for %s %s | Status: %s"
                                            (name (:request-method request))
                                            (:uri request)
                                            status)))
                   (assoc context :response updated-response)))))}))

(defn validate-request-body
  "Creates an interceptor to validate request body against a schema.
   
   Parameters:
   - schema: The Prismatic Schema to validate against
   - target-key: (optional) The key in the request where validated data will be placed.
                Defaults to :validated-wire
   
   Returns an interceptor that:
   - Expects JSON body to be already parsed and available in :json-params
   - Validates the body against the provided schema
   - Places validated data in the request under target-key
   - Returns 400 Bad Request if validation fails or body is missing"
  ([schema]
   (validate-request-body schema :validated-wire))
  ([schema target-key]
   (interceptor/interceptor
    {:name ::validate-request-body
     :enter (fn [context]
              (let [request (:request context)
                    json-body (:json-params request)
                    logger-comp (interceptors.components/get-component request :logger)
                    logger (when logger-comp (:logger logger-comp))]
                (if (nil? json-body)
                  (do
                    (when logger
                      (.warn logger (format "[Validation] Request body is required for %s %s"
                                            (name (:request-method request))
                                            (:uri request))))
                    (assoc context :response (response/bad-request "Request body is required")))
                  (try
                    (let [validated-wire (s/validate schema json-body)]
                      (when logger
                        (.debug logger (format "[Validation] Successfully validated request body for %s %s | Target key: %s"
                                               (name (:request-method request))
                                               (:uri request)
                                               target-key)))
                      (assoc-in context [:request target-key] validated-wire))
                    (catch clojure.lang.ExceptionInfo e
                      (let [error-message (or (.getMessage e) "Invalid request data")]
                        (when logger
                          (.warn logger (format "[Validation] Schema validation failed for %s %s: %s"
                                                (name (:request-method request))
                                                (:uri request)
                                                error-message)))
                        (assoc context :response (response/bad-request error-message))))
                    (catch Exception e
                      (when logger
                        (.error logger (format "[Validation] Unexpected validation error for %s %s: %s"
                                               (name (:request-method request))
                                               (:uri request)
                                               (.getMessage e))))
                      (assoc context :response (response/bad-request (str "Validation error: " (.getMessage e)))))))))})))

(def error-handler-interceptor
  "Interceptor to handle errors and return appropriate JSON responses.
   The json-response interceptor will automatically serialize the body."
  (interceptor/interceptor
   {:name ::error-handler
    :error (fn [context exception]
             (let [request (:request context)
                   logger-comp (interceptors.components/get-component request :logger)
                   logger (when logger-comp (:logger logger-comp))]
               (when logger
                 (.error logger (format "[Error Handler] Unhandled exception for %s %s: %s"
                                        (name (:request-method request))
                                        (:uri request)
                                        (.getMessage exception))
                         exception))
               (assoc context :response {:status 500
                                         :body {:error "Internal server error"}})))}))
