(ns challenge.interceptors.validation
  (:require [challenge.components.logger :as logger]
            [challenge.interceptors.components :as interceptors.components]
            [challenge.interface.http.response :as response]
            [cheshire.core :as json]
            [clojure.string :as string]
            [io.pedestal.interceptor :as interceptor]
            [schema.core :as s]))

(defn- content-type-is-json?
  "Returns true when the request Content-Type is application/json (or with charset)."
  [request]
  (when-let [content-type (or (get-in request [:headers "content-type"])
                              (get-in request [:headers "Content-Type"]))]
    (string/starts-with? (string/lower-case (str content-type)) "application/json")))

(def json-body
  "Interceptor to parse JSON request body and add it to :json-params key.
   Only parses when Content-Type is application/json; skips multipart and other types
   so the body remains available for other interceptors (e.g. import multipart)."
  (interceptor/interceptor
   {:name ::json-body
    :enter (fn [context]
             (let [request (:request context)
                   body (:body request)
                   logger-comp (interceptors.components/get-component request :logger)
                   log (logger/bound logger-comp)]
               (if (and body (content-type-is-json? request))
                 (try
                   (let [body-str (cond
                                    (string? body) body
                                    (instance? java.io.InputStream body) (slurp body)
                                    :else (str body))
                         parsed-body (when (and body-str (not (empty? body-str)))
                                       (json/parse-string body-str true))]
                     (if parsed-body
                       (do
                         (logger/log-call log :debug
                                          "[JSON Body] Successfully parsed JSON body for %s %s"
                                          (name (:request-method request))
                                          (:uri request))
                         (assoc-in context [:request :json-params] parsed-body))
                       context))
                   (catch Exception e
                     (logger/log-call log :warn
                                      "[JSON Body] Failed to parse JSON body for %s %s: %s"
                                      (name (:request-method request))
                                      (:uri request)
                                      (.getMessage e))
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
                   log (logger/bound logger-comp)]
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
                   (logger/log-call log :debug
                                    "[JSON Response] Serialized response body for %s %s | Status: %s"
                                    (name (:request-method request))
                                    (:uri request)
                                    status)
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
                    log (logger/bound logger-comp)]
                (if (nil? json-body)
                  (do
                    (logger/log-call log :warn
                                     "[Validation] Request body is required for %s %s"
                                     (name (:request-method request))
                                     (:uri request))
                    (assoc context :response (response/bad-request "Request body is required")))
                  (try
                    (let [validated-wire (s/validate schema json-body)]
                      (logger/log-call log :debug
                                       "[Validation] Successfully validated request body for %s %s | Target key: %s"
                                       (name (:request-method request))
                                       (:uri request)
                                       target-key)
                      (assoc-in context [:request target-key] validated-wire))
                    (catch clojure.lang.ExceptionInfo e
                      (let [error-message (or (.getMessage e) "Invalid request data")]
                        (logger/log-call log :warn
                                         "[Validation] Schema validation failed for %s %s: %s"
                                         (name (:request-method request))
                                         (:uri request)
                                         error-message)
                        (assoc context :response (response/bad-request error-message))))
                    (catch Exception e
                      (logger/log-call log :error
                                       "[Validation] Unexpected validation error for %s %s: %s | Exception type: %s | Stack trace: %s"
                                       (name (:request-method request))
                                       (:uri request)
                                       (.getMessage e)
                                       (type e)
                                       (pr-str (map str (.getStackTrace e))))
                      (assoc context :response (response/bad-request (str "Validation error: " (.getMessage e)))))))))})))

(def validate-path-params-id
  "Interceptor to validate and parse the :id path parameter as a Long.
   If the ID is present but invalid, throws NumberFormatException which will be
   caught by error-handler-interceptor and converted to 400 Bad Request.
   If valid, adds the parsed ID to the request as :activity-id for handlers to use."
  (interceptor/interceptor
   {:name ::validate-path-params-id
    :enter (fn [context]
             (let [request (:request context)
                   path-params (:path-params request)
                   id-str (get path-params :id)
                   logger-comp (interceptors.components/get-component request :logger)
                   log (logger/bound logger-comp)]
               (if (nil? id-str)
                 ;; No ID in path params, continue (for routes that don't require ID)
                 context
                 (let [activity-id (Long/parseLong id-str)]
                   (logger/log-call log :debug
                                    "[Path Params Validation] Successfully parsed activity ID: %s"
                                    activity-id)
                   (assoc-in context [:request :activity-id] activity-id)))))}))

(defn not-found-message?
  "Checks if an error message indicates a 'not found' condition.
   Uses case-insensitive matching for common patterns.
   Returns false if error-message is nil."
  [error-message]
  (if (nil? error-message)
    false
    (let [lower-message (string/lower-case error-message)]
      (or (string/includes? lower-message "not found")
          (string/includes? lower-message "does not exist")
          (string/includes? lower-message "not exist")))))

(def error-handler-interceptor
  "Interceptor to handle errors and return appropriate JSON responses.
   Handles:
   - clojure.lang.ExceptionInfo: Maps to 400 Bad Request or 404 Not Found based on message pattern
   - NumberFormatException: Maps to 400 Bad Request (invalid parameter format)
   - Other exceptions: Maps to 500 Internal Server Error
   The json-response interceptor will automatically serialize the body."
  (interceptor/interceptor
   {:name ::error-handler
    :error (fn [context exception]
             (let [request (:request context)
                   logger-comp (interceptors.components/get-component request :logger)
                   log (logger/bound logger-comp)
                   exception-type (type exception)
                   exception-message (.getMessage exception)
                   ;; Determine response based on exception type
                   response-map (cond
                                 ;; ExceptionInfo - could be validation error or not found
                                  (instance? clojure.lang.ExceptionInfo exception)
                                  (let [error-message exception-message]
                                    (if (not-found-message? error-message)
                                      (response/not-found error-message)
                                      (response/bad-request error-message)))

                                 ;; NumberFormatException - invalid parameter format
                                  (instance? NumberFormatException exception)
                                  (response/bad-request "Invalid parameter format")

                                 ;; Default - internal server error
                                  :else
                                  (response/internal-server-error "Internal server error"))
                   log-level (if (instance? clojure.lang.ExceptionInfo exception)
                               :warn
                               (if (instance? NumberFormatException exception)
                                 :warn
                                 :error))]
               (case log-level
                 :error (logger/log-call log :error
                                         "[Error Handler] Unhandled exception for %s %s | Type: %s | Message: %s | Exception: %s | Stack: %s"
                                         (name (:request-method request))
                                         (:uri request)
                                         exception-type
                                         exception-message
                                         (pr-str exception)
                                         (pr-str (take 5 (map str (.getStackTrace exception)))))
                 :warn (logger/log-call log :warn
                                        "[Error Handler] Exception for %s %s | Type: %s | Message: %s | Exception: %s"
                                        (name (:request-method request))
                                        (:uri request)
                                        exception-type
                                        exception-message
                                        (pr-str exception)))
               (assoc context :response response-map)))}))
