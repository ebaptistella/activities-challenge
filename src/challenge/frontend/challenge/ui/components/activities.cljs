(ns challenge.ui.components.activities
  "Components related to activity display.")

(defn loading-spinner
  "Loading component."
  []
  [:div.text-center.py-10
   [:div.inline-block.animate-spin.rounded-full.h-8.w-8.border-b-2.border-indigo-500]
   [:p.mt-4.text-gray-600 "Loading activities..."]])

(defn error-message
  "Error message component.
  
  Parameters:
  - error: String with error message"
  [error]
  [:div.p-4.rounded-lg.bg-red-50.text-red-700.border.border-red-200
   [:p.font-semibold "Error loading activities"]
   [:p.text-sm error]])

(defn empty-state
  "Empty state component."
  []
  [:div.text-center.py-10.text-gray-500
   "No activities found"])

(defn kind-badge
  "Badge component for activity type.
  
  Parameters:
  - kind: Keyword or string with activity type"
  [kind]
  (let [kind-str (if (keyword? kind) (name kind) (str kind))
        kind-label (case kind-str
                     "planned" "Planned"
                     "executed" "Executed"
                     kind-str)
        kind-class (case kind-str
                     "planned" "bg-blue-100 text-blue-800"
                     "executed" "bg-green-100 text-green-800"
                     "bg-gray-100 text-gray-800")]
    [:span.px-3.py-1.rounded-full.text-xs.font-semibold {:class kind-class}
     kind-label]))

(defn activity-row
  "Activity table row component.
  
  Parameters:
  - activity: Map with activity data"
  [activity]
  ^{:key (str (:activity activity) "-" (:activity_type activity) "-" (:kind activity))}
  [:tr.hover:bg-gray-50
   [:td.px-6.py-4.whitespace-nowrap.text-sm.text-gray-900 (:activity activity)]
   [:td.px-6.py-4.whitespace-nowrap.text-sm.text-gray-900 (:activity_type activity)]
   [:td.px-6.py-4.whitespace-nowrap.text-sm.text-gray-900 (:unit activity)]
   [:td.px-6.py-4.whitespace-nowrap.text-sm.text-gray-900 (:amount activity)]
   [:td.px-6.py-4.whitespace-nowrap.text-sm
    [kind-badge (:kind activity)]]])

(defn activities-table
  "Main activities table component.
  
  Parameters:
  - activities: Vector of maps with activities
  - loading: Boolean indicating if loading
  - error: String with error message or nil"
  [activities loading error]
  (cond
    loading
    [loading-spinner]

    error
    [error-message error]

    (empty? activities)
    [empty-state]

    :else
    [:div.overflow-x-auto.rounded-lg.border.border-gray-200
     [:table.min-w-full.divide-y.divide-gray-200
      [:thead.bg-gray-50
       [:tr
        [:th.px-6.py-3.text-left.text-xs.font-medium.text-gray-500.uppercase.tracking-wider "Activity"]
        [:th.px-6.py-3.text-left.text-xs.font-medium.text-gray-500.uppercase.tracking-wider "Activity Type"]
        [:th.px-6.py-3.text-left.text-xs.font-medium.text-gray-500.uppercase.tracking-wider "Unit"]
        [:th.px-6.py-3.text-left.text-xs.font-medium.text-gray-500.uppercase.tracking-wider "Amount"]
        [:th.px-6.py-3.text-left.text-xs.font-medium.text-gray-500.uppercase.tracking-wider "Type"]]]
      [:tbody.bg-white.divide-y.divide-gray-200
       (for [activity activities]
         [activity-row activity])]]]))
