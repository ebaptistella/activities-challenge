(ns challenge.ui.adapters
  "Adapters for data transformation between API and models.")

(defn api-response->activities
  "Converts activities API response to internal format.
  
  Parameters:
  - api-response: JavaScript object with API response
  
  Returns:
  - Vector of maps with activities in internal format (keywordized)"
  [api-response]
  (js->clj (.-items api-response) :keywordize-keys true))

(defn api-response->upload-summary
  "Converts upload API response to internal format.
  
  Parameters:
  - api-response: JavaScript object with API response
  
  Returns:
  - Map with :type (keyword), :valid, :invalid"
  [api-response]
  {:type (keyword (.-type api-response))
   :valid (.-valid api-response)
   :invalid (.-invalid api-response)})

(defn api-error->message
  "Extracts error message from API response.
  
  Parameters:
  - api-response: JavaScript object with error response
  
  Returns:
  - String with error message or nil"
  [api-response]
  (or (.-error api-response) nil))
