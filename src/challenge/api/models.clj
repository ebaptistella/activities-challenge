(ns challenge.api.models
  "Data models and domain entities.")

(defn activities-response
  "Creates an activities API response model.
  
  Parameters:
  - items: Vector of enriched activity maps
  
  Returns:
  - Map with items array"
  [items]
  {:items items})

(defn error-response
  "Creates an error response model.
  
  Parameters:
  - error: String with error message
  - details: Optional map with error details
  
  Returns:
  - Map with error response data"
  [error & [details]]
  (if details
    {:error error :details details}
    {:error error}))
