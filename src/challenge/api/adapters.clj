(ns challenge.api.adapters
  "Adapters for data transformation between wire formats and models.")

(defn model->wire-response
  "Converts domain model to wire response format.
  
  Parameters:
  - model: Map with domain data
  
  Returns:
  - Map ready for JSON serialization"
  [model]
  model)
