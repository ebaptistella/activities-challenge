(ns challenge.integration.aux.http-helpers
  (:require [cheshire.core :as json]
            [clojure.string :as str]
            [io.pedestal.http :as http]
            [io.pedestal.test :as pedestal-test]
            [state-flow.api :as flow :refer [flow]]))

(defn request
  "Makes an HTTP request to the Pedestal server.
   
   Options:
   - :method - HTTP method (:get, :post, :put, :delete, etc.)
   - :path - Request path (e.g., \"/activities\")
   - :body - Request body (map or string, will be JSON-encoded if map)
   - :headers - Optional headers map
   
   Returns a flow that produces a response map with:
   - :status - HTTP status code
   - :body - Response body (parsed JSON if content-type is application/json)
   - :headers - Response headers"
  [{:keys [method path body headers]}]
  (flow "make HTTP request"
        [pedestal (flow/get-state :pedestal)]
        (let [server-config (:server pedestal)
          ;; service-fn is created by http/create-server and should be in the config
          ;; After http/start, the service-fn is available at ::http/service-fn
              service-fn (::http/service-fn server-config)
              body-str (when body
                         (if (string? body)
                           body
                           (json/generate-string body)))
              request-headers (cond-> {}
                                body-str
                                (merge {"Content-Type" "application/json"})
                                headers
                                (merge headers))
              response (apply pedestal-test/response-for
                              (cond-> [service-fn method path]
                                body-str
                                (conj :body body-str)
                                (seq request-headers)
                                (conj :headers request-headers)))
              response-body (try
                              (if (and (string? (:body response))
                                       (or (nil? (get-in response [:headers "Content-Type"]))
                                           (str/includes?
                                            (get-in response [:headers "Content-Type"] "")
                                            "application/json")))
                                (json/parse-string (:body response) true)
                                (:body response))
                              (catch Exception _
                                (:body response)))]
          (flow/return (assoc response :body response-body)))))
