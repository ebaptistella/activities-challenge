(ns challenge.ui.logic
  "Pure business logic for the frontend."
  (:require [clojure.string :as str]))

(defn format-date
  "Formats a date string to YYYY-MM-DD format.
  
  Parameters:
  - date-str: ISO format date string or empty
  
  Returns:
  - Formatted string in YYYY-MM-DD format or empty string if date-str is empty"
  [date-str]
  (if (empty? date-str)
    ""
    (let [date (js/Date. date-str)]
      (str (.getFullYear date) "-"
           (-> (.getMonth date) inc str (.padStart 2 "0")) "-"
           (-> (.getDate date) str (.padStart 2 "0"))))))

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
  
  Parameters:
  - filters: Map with keys :date, :activity, :activity-type
  
  Returns:
  - String with formatted query parameters (e.g., 'date=2024-01-01&activity=test')"
  [filters]
  (let [date-value (:date filters)
        date-param (if (or (nil? date-value) (empty? (str date-value)))
                     (today-date)
                     (str date-value))
        params (->> [["date" date-param]
                     (when (not (empty? (:activity filters)))
                       ["activity" (:activity filters)])
                     (when (not (empty? (:activity-type filters)))
                       ["activity_type" (:activity-type filters)])]
                    (filter some?)
                    (map (fn [[k v]] (str k "=" (js/encodeURIComponent v))))
                    (str/join "&"))]
    params))

(defn activity-type-label
  "Returns a human-readable label for an activity type.
  
  Parameters:
  - type: Keyword or string representing the type
  
  Returns:
  - String with formatted label"
  [type]
  (let [type-str (if (keyword? type) (name type) (str type))]
    (case type-str
      "planned" "Planned"
      "executed" "Executed"
      type-str)))

(defn activity-type-class
  "Returns CSS classes for an activity type.
  
  Parameters:
  - type: Keyword or string representing the type
  
  Returns:
  - String with Tailwind CSS classes"
  [type]
  (let [type-str (if (keyword? type) (name type) (str type))]
    (case type-str
      "planned" "bg-blue-100 text-blue-800"
      "executed" "bg-green-100 text-green-800"
      "bg-gray-100 text-gray-800")))

(defn default-filters
  "Returns default filters with today's date.
  
  Returns:
  - Map with default filters"
  []
  {:date (today-date)
   :activity ""
   :activity-type ""})
