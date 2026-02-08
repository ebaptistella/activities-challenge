(ns challenge.ui.logic
  "Pure business logic for the frontend."
  (:require [clojure.string :as str]))

(defn today-date
  "Returns today's date in YYYY-MM-DD format.
  
  Returns:
  - String with current date in YYYY-MM-DD format"
  []
  (let [now (js/Date.)]
    (str (.getFullYear now) "-"
         (-> (.getMonth now) inc str (.padStart 2 "0")) "-"
         (-> (.getDate now) str (.padStart 2 "0")))))

(defn build-query-params
  "Builds query string parameters from a filters map.
   Only includes params for non-empty values; when date is empty, no date param is sent.
  
  Parameters:
  - filters: Map with keys :date, :activity, :activity-type
  
  Returns:
  - String with formatted query parameters (e.g., 'date=2024-01-01&activity=test')"
  [filters]
  (let [date-value (:date filters)
        date-param (when (and (some? date-value) (not (str/blank? (str date-value))))
                     (str date-value))
        params (->> [(when date-param ["date" date-param])
                     (when (not (str/blank? (str (:activity filters))))
                       ["activity" (:activity filters)])
                     (when (not (str/blank? (str (:activity-type filters))))
                       ["activity_type" (:activity-type filters)])]
                    (filter some?)
                    (map (fn [[k v]] (str k "=" (js/encodeURIComponent v))))
                    (str/join "&"))]
    params))

(defn default-filters
  "Returns default filters with empty date (no date param sent until user chooses one).
  
  Returns:
  - Map with default filters"
  []
  {:date ""
   :activity ""
   :activity-type ""})
