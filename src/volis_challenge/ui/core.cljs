(ns volis-challenge.ui.core
  (:require [clojure.string :as str]
            [reagent.core :as r]
            [reagent.dom :as rdom]))

(defonce app-state (r/atom {:filters {:date ""
                                      :activity ""
                                      :activity-type ""}
                            :upload-status nil
                            :activities []
                            :activities-loading false
                            :activities-error nil}))

(defn format-date
  [date-str]
  (if (empty? date-str)
    ""
    (let [date (js/Date. date-str)]
      (str (.getFullYear date) "-"
           (-> (.getMonth date) inc str (.padStart 2 "0")) "-"
           (-> (.getDate date) str (.padStart 2 "0"))))))

(defn today-date
  []
  (let [now (js/Date.)]
    (str (.getFullYear now) "-"
         (-> (.getMonth now) inc str (.padStart 2 "0")) "-"
         (-> (.getDate now) str (.padStart 2 "0")))))

(defn update-filter!
  [key value]
  (swap! app-state update-in [:filters key] (constantly value)))

(defn fetch-activities!
  []
  (let [filters (:filters @app-state)
        date-param (if (empty? (:date filters))
                     (today-date)
                     (:date filters))
        params (->> [["date" date-param]
                     (when (not (empty? (:activity filters)))
                       ["activity" (:activity filters)])
                     (when (not (empty? (:activity-type filters)))
                       ["activity_type" (:activity-type filters)])]
                    (filter some?)
                    (map (fn [[k v]] (str k "=" (js/encodeURIComponent v))))
                    (str/join "&"))
        url (str "/api/activities?" params)]
    (swap! app-state assoc :activities-loading true :activities-error nil)
    (-> (js/fetch url)
        (.then (fn [response]
                 (let [content-type (-> response .-headers (.get "content-type") (or ""))
                       is-json (or (.includes content-type "application/json")
                                   (.includes content-type "text/json"))]
                   (if (>= (.-status response) 200)
                     (if (< (.-status response) 300)
                       (if is-json
                         (-> (.json response)
                             (.then (fn [data]
                                      (swap! app-state assoc
                                             :activities (js->clj (.-items data) :keywordize-keys true)
                                             :activities-loading false
                                             :activities-error nil)))
                             (.catch (fn [error]
                                      (swap! app-state assoc
                                             :activities-loading false
                                             :activities-error (str "Erro ao processar JSON: " (or (.-message error) "Resposta inválida"))))))
                         (-> (.text response)
                             (.then (fn [_]
                                      (swap! app-state assoc
                                             :activities-loading false
                                             :activities-error (str "Resposta não é JSON. Status: " (.-status response)))))))
                       (if is-json
                         (-> (.json response)
                             (.then (fn [data]
                                      (swap! app-state assoc
                                             :activities-loading false
                                             :activities-error (or (.-error data) "Erro ao buscar atividades"))))
                             (.catch (fn [_]
                                      (swap! app-state assoc
                                             :activities-loading false
                                             :activities-error (str "Erro HTTP " (.-status response))))))
                         (-> (.text response)
                             (.then (fn [text]
                                      (swap! app-state assoc
                                             :activities-loading false
                                             :activities-error (str "Erro HTTP " (.-status response) ": " (subs text 0 (min 100 (count text))))))))))
                     (swap! app-state assoc
                            :activities-loading false
                            :activities-error "Erro de conexão")))))
        (.catch (fn [error]
                 (swap! app-state assoc
                        :activities-loading false
                        :activities-error (str "Erro ao buscar atividades: " (or (.-message error) "Erro desconhecido"))))))))

(defn upload-csv!
  [file]
  (let [form-data (js/FormData.)]
    (.append form-data "file" file)
    (swap! app-state assoc :upload-status {:loading true})
    (-> (js/fetch "/api/import"
                  {:method "POST"
                   :body form-data})
        (.then (fn [response]
                 (if (>= (.-status response) 200)
                   (if (< (.-status response) 300)
                     (-> (.json response)
                         (.then (fn [data]
                                  (swap! app-state assoc
                                         :upload-status {:success true
                                                         :type (keyword (.-type data))
                                                         :valid (.-valid data)
                                                         :invalid (.-invalid data)
                                                         :loading false})
                                  (fetch-activities!)
                                  (js/setTimeout #(swap! app-state assoc :upload-status nil) 5000))))
                     (-> (.json response)
                         (.then (fn [data]
                                  (swap! app-state assoc
                                         :upload-status {:error true
                                                         :message (or (.-error data) "Erro ao fazer upload")
                                                         :loading false})
                                  (js/setTimeout #(swap! app-state assoc :upload-status nil) 5000)))))
                   (do
                     (swap! app-state assoc
                            :upload-status {:error true
                                            :message "Erro de conexão"
                                            :loading false})
                     (js/setTimeout #(swap! app-state assoc :upload-status nil) 5000)))))
        (.catch (fn [error]
                 (swap! app-state assoc
                        :upload-status {:error true
                                        :message (or (.-message error) "Erro desconhecido")
                                        :loading false})
                 (js/setTimeout #(swap! app-state assoc :upload-status nil) 5000))))))

(defn handle-file-select
  [event]
  (let [file (-> event .-target .-files (aget 0))]
    (when file
      (upload-csv! file))))

(defn handle-filter-change
  [key event]
  (let [value (-> event .-target .-value)]
    (update-filter! key value)
    (when (= key :date)
      (fetch-activities!))))

(defn handle-apply-filters
  []
  (fetch-activities!))

(defn upload-status-message
  [status]
  (when status
    (cond
      (:loading status)
      [:div.mt-5.p-4.rounded-lg.text-sm.bg-blue-50.text-blue-700.border.border-blue-200
       "Enviando arquivo..."]
      (:success status)
      (let [type-name (case (:type status)
                        :planned "Planejado"
                        :executed "Executado"
                        "Desconhecido")]
        [:div.mt-5.p-4.rounded-lg.text-sm.bg-green-50.text-green-700.border.border-green-200
         (str "Upload realizado com sucesso! Tipo detectado: " type-name
              " | Válidos: " (:valid status)
              " | Inválidos: " (:invalid status))])
      (:error status)
      [:div.mt-5.p-4.rounded-lg.text-sm.bg-red-50.text-red-700.border.border-red-200
       (str "Erro no upload: " (or (:message status) "Erro desconhecido"))])))

(defn render-filters
  []
  (let [filters (:filters @app-state)]
    [:div.grid.grid-cols-1.md:grid-cols-2.lg:grid-cols-4.gap-5.items-end
     [:div.flex.flex-col.gap-2
      [:label.font-semibold.text-gray-800.text-sm {:for "date-filter"} "Data:"]
      [:input#date-filter.p-3.border-2.border-gray-300.rounded-md.text-base.transition-colors.focus:outline-none.focus:border-indigo-500
       {:type "date"
        :value (:date filters)
        :on-change #(handle-filter-change :date %)}]]
     [:div.flex.flex-col.gap-2
      [:label.font-semibold.text-gray-800.text-sm {:for "activity-filter"} "Atividade:"]
      [:input#activity-filter.p-3.border-2.border-gray-300.rounded-md.text-base.transition-colors.focus:outline-none.focus:border-indigo-500
       {:type "text"
        :placeholder "Filtrar por atividade"
        :value (:activity filters)
        :on-input #(update-filter! :activity (-> % .-target .-value))}]]
     [:div.flex.flex-col.gap-2
      [:label.font-semibold.text-gray-800.text-sm {:for "activity-type-filter"} "Tipo de Atividade:"]
      [:input#activity-type-filter.p-3.border-2.border-gray-300.rounded-md.text-base.transition-colors.focus:outline-none.focus:border-indigo-500
       {:type "text"
        :placeholder "Filtrar por tipo"
        :value (:activity-type filters)
        :on-input #(update-filter! :activity-type (-> % .-target .-value))}]]
     [:div.flex.flex-col.gap-2
      [:button#apply-filters-btn.p-3.px-6.border-none.rounded-md.text-base.font-semibold.cursor-pointer.transition-all.bg-gradient-to-r.from-indigo-500.to-purple-600.text-white.hover:-translate-y-0.5.hover:shadow-lg.active:translate-y-0
       {:on-click handle-apply-filters}
       "Aplicar Filtros"]]]))

(defn render-upload
  []
  (let [status (:upload-status @app-state)]
    [:div
     [:div.border-2.border-dashed.border-indigo-500.rounded-lg.p-10.text-center.bg-indigo-50.transition-all.cursor-pointer.hover:border-purple-600.hover:bg-indigo-100
      [:label.flex.flex-col.items-center.gap-4.cursor-pointer {:for "csv-upload"}
       [:svg {:width 48 :height 48 :viewBox "0 0 24 24" :fill "none" :stroke "currentColor" :stroke-width 2 :class "text-indigo-500"}
        [:path {:d "M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"}]
        [:polyline {:points "17 8 12 3 7 8"}]
        [:line {:x1 12 :y1 3 :x2 12 :y2 15}]]
       [:span.text-xl.font-semibold.text-gray-800 "Clique para fazer upload de CSV"]
       [:small.text-sm.text-gray-600 "Arquivos planned ou executed são aceitos"]]
      [:input#csv-upload.hidden {:type "file" :accept ".csv" :on-change handle-file-select}]]
     (upload-status-message status)]))

(defn kind-badge
  [kind]
  (let [kind-str (if (keyword? kind) (name kind) kind)
        kind-label (case kind-str
                     "planned" "Planejado"
                     "executed" "Executado"
                     "both" "Ambos"
                     kind-str)
        kind-class (case kind-str
                     "planned" "bg-blue-100 text-blue-800"
                     "executed" "bg-green-100 text-green-800"
                     "both" "bg-purple-100 text-purple-800"
                     "bg-gray-100 text-gray-800")]
    [:span.px-3.py-1.rounded-full.text-xs.font-semibold {:class kind-class}
     kind-label]))

(defn activities-table
  []
  (let [activities (:activities @app-state)
        loading (:activities-loading @app-state)
        error (:activities-error @app-state)]
    (cond
      loading
      [:div.text-center.py-10
       [:div.inline-block.animate-spin.rounded-full.h-8.w-8.border-b-2.border-indigo-500]
       [:p.mt-4.text-gray-600 "Carregando atividades..."]]
      error
      [:div.p-4.rounded-lg.bg-red-50.text-red-700.border.border-red-200
       [:p.font-semibold "Erro ao carregar atividades"]
       [:p.text-sm error]]
      (empty? activities)
      [:div.text-center.py-10.text-gray-500
       "Nenhuma atividade encontrada"]
      :else
      [:div.overflow-x-auto.rounded-lg.border.border-gray-200
       [:table.min-w-full.divide-y.divide-gray-200
        [:thead.bg-gray-50
         [:tr
          [:th.px-6.py-3.text-left.text-xs.font-medium.text-gray-500.uppercase.tracking-wider "Atividade"]
          [:th.px-6.py-3.text-left.text-xs.font-medium.text-gray-500.uppercase.tracking-wider "Tipo de Atividade"]
          [:th.px-6.py-3.text-left.text-xs.font-medium.text-gray-500.uppercase.tracking-wider "Unidade"]
          [:th.px-6.py-3.text-left.text-xs.font-medium.text-gray-500.uppercase.tracking-wider "Quantidade"]
          [:th.px-6.py-3.text-left.text-xs.font-medium.text-gray-500.uppercase.tracking-wider "Tipo"]]]
        [:tbody.bg-white.divide-y.divide-gray-200
         (for [activity activities]
           ^{:key (str (:activity activity) "-" (:activity_type activity) "-" (:kind activity))}
           [:tr.hover:bg-gray-50
            [:td.px-6.py-4.whitespace-nowrap.text-sm.text-gray-900 (:activity activity)]
            [:td.px-6.py-4.whitespace-nowrap.text-sm.text-gray-900 (:activity_type activity)]
            [:td.px-6.py-4.whitespace-nowrap.text-sm.text-gray-900 (:unit activity)]
            [:td.px-6.py-4.whitespace-nowrap.text-sm.text-gray-900 (:amount activity)]
            [:td.px-6.py-4.whitespace-nowrap.text-sm
             (kind-badge (:kind activity))]])]]])))

(defn render-app
  []
  [:div.max-w-6xl.mx-auto.bg-white.rounded-xl.shadow-2xl.overflow-hidden
   [:header.bg-gradient-to-r.from-indigo-500.to-purple-600.text-white.p-8.text-center
    [:h1.text-4xl.md:text-5xl.mb-2.5 "Volis Challenge"]
    [:p.text-lg.opacity-90 "Gerenciamento de Atividades Planejadas e Executadas"]]
   [:main.p-8.md:p-8
    [:section.mb-10.upload-section
     [:h2.text-2xl.mb-5.text-gray-800 "Upload de Arquivos CSV"]
     [render-upload]]
    [:section.mb-10.filters-section
     [:h2.text-2xl.mb-5.text-gray-800 "Filtros"]
     [render-filters]]
    [:section.mb-10.activities-section
     [:h2.text-2xl.mb-5.text-gray-800 "Atividades"]
     [activities-table]]]])

(defn mount-root
  []
  (when-let [app-el (.getElementById js/document "app")]
    (rdom/render [render-app] app-el)
    (let [filters (:filters @app-state)]
      (when (empty? (:date filters))
        (update-filter! :date (today-date))
        (fetch-activities!)))))

(defn ^:export init
  []
  (mount-root))
