(ns challenge.ui.adapters
  "Adapters for data transformation between API and models.")

(defn- normalize-activity-item
  "Normalizes a single activity from API (supports kebab-case, snake_case and camelCase from JSON).
   Ensures keys used by the table exist and adds :amount-display and :kind for display."
  [item]
  (let [m (if (map? item) item (js->clj item :keywordize-keys true))
        activity-type (or (:activity-type m) (:activity_type m))
        amount-planned (or (:amount-planned m) (:amount_planned m))
        amount-executed (or (:amount-executed m) (:amount_executed m))
        amount-display (cond
                         (and (some? amount-planned) (some? amount-executed))
                         (str amount-planned " / " amount-executed)
                         (some? amount-executed) (str amount-executed)
                         (some? amount-planned) (str amount-planned)
                         :else "-")
        kind (if (and (some? amount-executed) (number? amount-executed)) :executed :planned)]
    (assoc m
           :activity-type activity-type
           :activity_type activity-type
           :amount-planned amount-planned
           :amount-executed amount-executed
           :amount-display amount-display
           :kind kind)))

(defn api-response->activities
  "Converts activities list API response to internal format.
   API returns { items: [...] }. Each item has activity, activity-type, unit, amount-planned, amount-executed.
  
  Parameters:
  - api-response: JavaScript object with API response (root key .items)
  
  Returns:
  - Vector of maps with activities in internal format (keywordized and normalized)"
  [api-response]
  (let [items (or (.-items api-response) #js [])]
    (mapv normalize-activity-item (js->clj items :keywordize-keys true))))

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
