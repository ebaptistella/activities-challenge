(ns challenge.ui.models
  "Data models and application state structure.")

(def initial-state
  "Initial application state."
  {:filters {:date ""
             :activity ""
             :activity-type ""}
   :upload-status nil
   :activities []
   :activities-loading false
   :activities-error nil})

(defn filters
  "Returns filters from state."
  [state]
  (:filters state))

(defn upload-status
  "Returns upload status from state."
  [state]
  (:upload-status state))

(defn activities
  "Returns activities from state."
  [state]
  (:activities state))

(defn activities-loading?
  "Checks if activities are being loaded."
  [state]
  (:activities-loading state))

(defn activities-error
  "Returns activities loading error, if any."
  [state]
  (:activities-error state))
