(ns challenge.ui.core
  "Main namespace for application orchestration and initialization."
  (:require [reagent.core :as r]
            [reagent.dom :as rdom]
            [challenge.ui.models :as models]
            [challenge.ui.logic :as logic]
            [challenge.ui.adapters :as adapters]
            [challenge.ui.http-client :as http]
            [challenge.ui.components.filters :as filters]
            [challenge.ui.components.upload :as upload]
            [challenge.ui.components.activities :as activities]))

(defonce app-state (r/atom models/initial-state))

(defn update-filter!
  "Updates a specific filter in state.
  
  Parameters:
  - key: Keyword with filter key (:date, :activity, :activity-type)
  - value: Value to be assigned"
  [key value]
  (swap! app-state update-in [:filters key] (constantly value)))

(defn set-activities-loading!
  "Sets activities loading state.
  
  Parameters:
  - loading: Boolean indicating if loading"
  [loading]
  (swap! app-state assoc :activities-loading loading :activities-error nil))

(defn set-activities!
  "Sets activities in state.
  
  Parameters:
  - activities-data: Vector of maps with activities"
  [activities-data]
  (swap! app-state assoc
         :activities activities-data
         :activities-loading false
         :activities-error nil))

(defn set-activities-error!
  "Sets an error in activities loading.
  
  Parameters:
  - error-message: String with error message"
  [error-message]
  (swap! app-state assoc
         :activities-loading false
         :activities-error error-message))

(defn set-upload-status!
  "Sets upload status.
  
  Parameters:
  - status: Map with upload status"
  [status]
  (swap! app-state assoc :upload-status status))

(defn clear-upload-status!
  "Clears upload status after a delay."
  []
  (js/setTimeout #(swap! app-state assoc :upload-status nil) 5000))

(defn fetch-activities!
  "Fetches activities from API with current filters."
  []
  (let [filters (models/filters @app-state)
        query-params (logic/build-query-params filters)]
    (set-activities-loading! true)
    (http/fetch-activities query-params
                          (fn [data]
                            (let [activities-data (adapters/api-response->activities data)]
                              (set-activities! activities-data)))
                          (fn [error-msg]
                            (set-activities-error! error-msg)))))

(defn handle-upload-success
  "Processes file upload success.
  
  Parameters:
  - data: JavaScript object with response data"
  [data]
  (let [summary (adapters/api-response->upload-summary data)]
    (set-upload-status! {:success true
                          :type (:type summary)
                          :valid (:valid summary)
                          :invalid (:invalid summary)
                          :loading false})
    (fetch-activities!)
    (clear-upload-status!)))

(defn handle-upload-error
  "Processes file upload error.
  
  Parameters:
  - message: String with error message"
  [message]
  (set-upload-status! {:error true
                        :message message
                        :loading false})
  (clear-upload-status!))

(defn upload-csv!
  "Uploads a CSV file.
  
  Parameters:
  - file: JavaScript File object"
  [file]
  (set-upload-status! {:loading true})
  (http/upload-csv file
                   handle-upload-success
                   handle-upload-error))

(defn handle-file-select
  "Handles file selection.
  
  Parameters:
  - event: Input file event"
  [event]
  (let [file (-> event .-target .-files (aget 0))]
    (when file
      (upload-csv! file))))

(defn handle-filter-change
  "Handles filter change.
  
  Parameters:
  - key: Keyword with filter key
  - value: New filter value"
  [key value]
  (let [date-value (if (= key :date) (str value) value)]
    (update-filter! key date-value)
    (when (= key :date)
      (fetch-activities!))))

(defn handle-apply-filters
  "Applies current filters."
  []
  (fetch-activities!))

(defn clear-filters!
  "Clears all filters and fetches activities with default filters."
  []
  (swap! app-state assoc :filters (logic/default-filters))
  (fetch-activities!))

(defn app
  "Main application component."
  []
  (let [state @app-state
        filters-data (models/filters state)
        upload-status-data (models/upload-status state)
        activities-data (models/activities state)
        loading? (models/activities-loading? state)
        error-msg (models/activities-error state)]
    [:div.max-w-6xl.mx-auto.bg-white.rounded-xl.shadow-2xl.overflow-hidden
     [:header.bg-gradient-to-r.from-indigo-500.to-purple-600.text-white.p-8.text-center
      [:h1.text-4xl.md:text-5xl.mb-2.5 "Challenge"]
      [:p.text-lg.opacity-90 "Planned and Executed Activities Management"]]
     [:main.p-8.md:p-8
      [:section.mb-10.upload-section
       [:h2.text-2xl.mb-5.text-gray-800 "CSV File Upload"]
       [upload/upload-section upload-status-data handle-file-select]]
      [:section.mb-10.filters-section
       [:h2.text-2xl.mb-5.text-gray-800 "Filters"]
       [filters/filters-section filters-data handle-filter-change handle-apply-filters clear-filters!]]
      [:section.mb-10.activities-section
       [:h2.text-2xl.mb-5.text-gray-800 "Activities"]
       [activities/activities-table activities-data loading? error-msg]]]]))

(defn mount-root
  "Mounts root application component and initializes state."
  []
  (when-let [app-el (.getElementById js/document "app")]
    (rdom/render [app] app-el)
    (let [current-filters (models/filters @app-state)]
      (when (empty? (:date current-filters))
        (let [today (logic/today-date)]
          (update-filter! :date today)
          (fetch-activities!))))))

(defn ^:export init
  "Exported initialization function to be called by HTML."
  []
  (mount-root))
