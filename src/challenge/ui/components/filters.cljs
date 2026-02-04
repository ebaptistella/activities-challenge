(ns challenge.ui.components.filters
  "Components related to activity filters.")

(defn filter-input
  "Filter input component.
  
  Parameters:
  - id: String with input ID
  - label: String with input label
  - type: String with input type (date, text, etc.)
  - value: String with current value
  - placeholder: Optional string with placeholder
  - on-change: Function called when value changes"
  [id label type value placeholder on-change]
  [:div.flex.flex-col.gap-2
   [:label.font-semibold.text-gray-800.text-sm {:for id} (str label ":")]
   [:input {:id id
            :class "p-3 border-2 border-gray-300 rounded-md text-base transition-colors focus:outline-none focus:border-indigo-500"
            :type type
            :value value
            :placeholder placeholder
            :on-change on-change}]])

(defn apply-button
  "Button to apply filters."
  [on-click]
  [:div.flex.flex-col.gap-2
   [:button {:id "apply-filters-btn"
             :class "p-3 px-6 border-none rounded-md text-base font-semibold cursor-pointer transition-all bg-gradient-to-r from-indigo-500 to-purple-600 text-white hover:-translate-y-0.5 hover:shadow-lg active:translate-y-0"
             :on-click on-click}
    "Apply Filters"]])

(defn clear-button
  "Button to clear filters."
  [on-click]
  [:div.flex.flex-col.gap-2
   [:button {:id "clear-filters-btn"
             :class "p-3 px-6 border-2 border-gray-300 rounded-md text-base font-semibold cursor-pointer transition-all bg-white text-gray-700 hover:bg-gray-50 hover:border-gray-400 active:bg-gray-100"
             :on-click on-click}
    "Clear Filters"]])

(defn filters-section
  "Main filters component.
  
  Parameters:
  - filters: Map with current filters
  - on-filter-change: Function called when a filter changes (receives key and value)
  - on-apply: Function called when Apply button is clicked
  - on-clear: Function called when Clear button is clicked"
  [filters on-filter-change on-apply on-clear]
  [:div.grid.grid-cols-1.md:grid-cols-2.lg:grid-cols-5.gap-5.items-end
   [filter-input "date-filter" "Date" "date" (:date filters)
    nil
    #(on-filter-change :date (-> % .-target .-value))]
   [filter-input "activity-filter" "Activity" "text" (:activity filters)
    "Filter by activity"
    #(on-filter-change :activity (-> % .-target .-value))]
   [filter-input "activity-type-filter" "Activity Type" "text" (:activity-type filters)
    "Filter by type"
    #(on-filter-change :activity-type (-> % .-target .-value))]
   [apply-button on-apply]
   [clear-button on-clear]])
