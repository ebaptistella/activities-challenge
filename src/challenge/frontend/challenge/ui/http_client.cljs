(ns challenge.ui.http-client
  "HTTP client for API communication with robust error handling.")

(defn is-json-response?
  "Checks if response is JSON type.
  
  Parameters:
  - response: fetch API Response object
  
  Returns:
  - Boolean indicating if it's JSON"
  [response]
  (let [content-type (-> response .-headers (.get "content-type") (or ""))]
    (or (.includes content-type "application/json")
        (.includes content-type "text/json"))))

(defn is-success-status?
  "Checks if HTTP status indicates success.
  
  Parameters:
  - status: HTTP status number
  
  Returns:
  - Boolean indicating if it's a success status (200-299)"
  [status]
  (and (>= status 200) (< status 300)))

(defn parse-json-response
  "Parses a JSON response.
  
  Parameters:
  - response: fetch API Response object
  
  Returns:
  - Promise that resolves with parsed JavaScript object"
  [response]
  (.json response))

(defn parse-text-response
  "Parses a text response.
  
  Parameters:
  - response: fetch API Response object
  
  Returns:
  - Promise that resolves with text string"
  [response]
  (.text response))

(defn handle-json-response
  "Processes a JSON response based on status.
  
  Parameters:
  - response: fetch API Response object
  - status: HTTP status number
  - on-success: Function called with data on success
  - on-error: Function called with error message on failure
  
  Returns:
  - Promise"
  [response status on-success on-error]
  (-> (parse-json-response response)
      (.then (fn [data]
               (if (is-success-status? status)
                 (on-success data)
                 (on-error (or (.-error data) (str "HTTP Error " status))))))
      (.catch (fn [error]
                (on-error (str "Error processing JSON: " (or (.-message error) "Invalid response")))))))

(defn handle-text-response
  "Processes a text response based on status.
  
  Parameters:
  - response: fetch API Response object
  - status: HTTP status number
  - on-error: Function called with error message
  
  Returns:
  - Promise"
  [response status on-error]
  (-> (parse-text-response response)
      (.then (fn [text]
               (let [truncated-text (subs text 0 (min 200 (count text)))]
                 (if (is-success-status? status)
                   (on-error (str "Response is not JSON. Status: " status ". Response: " truncated-text))
                   (on-error (str "HTTP Error " status ": " truncated-text))))))))

(defn fetch-json
  "Makes an HTTP request and returns a promise that resolves with JSON data or rejects with error.
  
  Parameters:
  - url: String with request URL
  - options: Optional map with fetch options (method, body, etc.)
  - on-success: Function called with data on success
  - on-error: Function called with error message on failure
  
  Returns:
  - Promise"
  [url options on-success on-error]
  (-> (js/fetch url (clj->js options))
      (.then (fn [response]
               (let [status (.-status response)]
                 (if (>= status 200)
                   (if (is-json-response? response)
                     (handle-json-response response status on-success on-error)
                     (handle-text-response response status on-error))
                   (on-error "Connection error")))))
      (.catch (fn [error]
                (on-error (str "Error fetching: " (or (.-message error) "Unknown error")))))))

(defn fetch-activities
  "Makes request to fetch activities with filters.
  
  Parameters:
  - query-params: String with query parameters
  - on-success: Function called with data on success
  - on-error: Function called with error message on failure
  
  Returns:
  - Promise"
  [query-params on-success on-error]
  (let [url (str "/api/activities?" query-params)]
    (fetch-json url {:method "GET"} on-success on-error)))

(defn upload-csv
  "Uploads a CSV file.
  
  Parameters:
  - file: JavaScript File object
  - on-success: Function called with data on success
  - on-error: Function called with error message on failure
  
  Returns:
  - Promise"
  [file on-success on-error]
  (let [form-data (js/FormData.)]
    (.append form-data "file" file)
    (fetch-json "/api/import"
                {:method "POST"
                 :body form-data}
                on-success
                on-error)))
