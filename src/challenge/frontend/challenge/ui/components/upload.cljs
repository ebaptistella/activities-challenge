(ns challenge.ui.components.upload
  "Components related to CSV file uploads.")

(defn upload-icon
  "SVG icon for upload."
  []
  [:svg {:width 48 :height 48 :viewBox "0 0 24 24" :fill "none" :stroke "currentColor" :stroke-width 2 :class "text-indigo-500"}
   [:path {:d "M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"}]
   [:polyline {:points "17 8 12 3 7 8"}]
   [:line {:x1 12 :y1 3 :x2 12 :y2 15}]])

(defn status-message
  "Upload status message component.
  
  Parameters:
  - status: Map with :loading, :success, :error, :type, :valid, :invalid, :message"
  [status]
  (when status
    (cond
      (:loading status)
      [:div.mt-5.p-4.rounded-lg.text-sm.bg-blue-50.text-blue-700.border.border-blue-200
       "Uploading file..."]

      (:success status)
      (let [type-name (case (:type status)
                        :planned "Planned"
                        :executed "Executed"
                        "Unknown")]
        [:div.mt-5.p-4.rounded-lg.text-sm.bg-green-50.text-green-700.border.border-green-200
         (str "Upload completed successfully! Detected type: " type-name
              " | Valid: " (:valid status)
              " | Invalid: " (:invalid status))])

      (:error status)
      [:div.mt-5.p-4.rounded-lg.text-sm.bg-red-50.text-red-700.border.border-red-200
       (str "Upload error: " (or (:message status) "Unknown error"))])))

(defn upload-section
  "Main upload component.
  
  Parameters:
  - status: Map with upload status
  - on-file-select: Function called when a file is selected"
  [status on-file-select]
  [:div
   [:div.border-2.border-dashed.border-indigo-500.rounded-lg.p-10.text-center.bg-indigo-50.transition-all.cursor-pointer.hover:border-purple-600.hover:bg-indigo-100
    [:label.flex.flex-col.items-center.gap-4.cursor-pointer {:for "csv-upload"}
     [upload-icon]
     [:span.text-xl.font-semibold.text-gray-800 "Click to upload CSV file"]
     [:small.text-sm.text-gray-600 "Planned or executed files are accepted"]]
    [:input#csv-upload.hidden {:type "file" :accept ".csv" :on-change on-file-select}]]
   [status-message status]])
