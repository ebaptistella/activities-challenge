(ns volis-challenge.ui.core
  (:require [clojure.string :as str]))

(defonce app-state (atom {:filters {:date ""
                                    :activity ""
                                    :activity-type ""}
                          :upload-status nil
                          :activities []}))

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

(defn upload-csv!
  [file]
  (let [form-data (js/FormData.)]
    (.append form-data "file" file)
    (swap! app-state assoc :upload-status {:loading true})
    (-> (js/fetch "/api/import"
                  {:method "POST"
                   :body form-data})
        (.then (fn [response]
                 (-> (.json response)
                     (.then (fn [data]
                              (swap! app-state assoc
                                     :upload-status {:success true
                                                     :type (keyword (.-type data))
                                                     :valid (.-valid data)
                                                     :invalid (.-invalid data)
                                                     :loading false})
                              (js/setTimeout #(swap! app-state assoc :upload-status nil) 5000))))))
        (.catch (fn [error]
                  (swap! app-state assoc
                         :upload-status {:error true
                                         :message (.-message error)
                                         :loading false})
                  (js/setTimeout #(swap! app-state assoc :upload-status nil) 5000))))))

(defn fetch-activities!
  []
  (let [filters (:filters @app-state)
        params (->> [["date" (:date filters)]
                     ["activity" (:activity filters)]
                     ["activity_type" (:activity-type filters)]]
                    (filter (fn [[_ v]] (not (empty? v))))
                    (map (fn [[k v]] (str k "=" (js/encodeURIComponent v))))
                    (str/join "&"))
        url (if (empty? params)
              "/api/activities"
              (str "/api/activities?" params))]
    (-> (js/fetch url)
        (.then (fn [response]
                 (-> (.json response)
                     (.then (fn [data]
                              (swap! app-state assoc :activities (.-items data)))))))
        (.catch (fn [error]
                  (js/console.error "Erro ao buscar atividades:" error))))))

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
    (let [status-el (.createElement js/document "div")]
      (cond
        (:loading status)
        (do
          (set! (.-textContent status-el) "Enviando arquivo...")
          (set! (.-className status-el) "mt-5 p-4 rounded-lg text-sm bg-blue-50 text-blue-700 border border-blue-200"))
        (:success status)
        (let [type-name (case (:type status)
                          :planned "Planejado"
                          :executed "Executado"
                          "Desconhecido")]
          (set! (.-textContent status-el)
                (str "Upload realizado com sucesso! Tipo detectado: " type-name
                     " | Válidos: " (:valid status)
                     " | Inválidos: " (:invalid status)))
          (set! (.-className status-el) "mt-5 p-4 rounded-lg text-sm bg-green-50 text-green-700 border border-green-200"))
        (:error status)
        (do
          (set! (.-textContent status-el)
                (str "Erro no upload: " (or (:message status) "Erro desconhecido")))
          (set! (.-className status-el) "mt-5 p-4 rounded-lg text-sm bg-red-50 text-red-700 border border-red-200")))
      status-el)))

(defn render-filters
  []
  (let [filters (:filters @app-state)
        container (.createElement js/document "div")]
    (set! (.-className container) "grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-5 items-end")
    (set! (.-innerHTML container)
          (str "<div class=\"flex flex-col gap-2\">"
               "<label for=\"date-filter\" class=\"font-semibold text-gray-800 text-sm\">Data:</label>"
               "<input type=\"date\" id=\"date-filter\" class=\"p-3 border-2 border-gray-300 rounded-md text-base transition-colors focus:outline-none focus:border-indigo-500\" value=\"" (:date filters) "\" />"
               "</div>"
               "<div class=\"flex flex-col gap-2\">"
               "<label for=\"activity-filter\" class=\"font-semibold text-gray-800 text-sm\">Atividade:</label>"
               "<input type=\"text\" id=\"activity-filter\" class=\"p-3 border-2 border-gray-300 rounded-md text-base transition-colors focus:outline-none focus:border-indigo-500\" placeholder=\"Filtrar por atividade\" value=\"" (:activity filters) "\" />"
               "</div>"
               "<div class=\"flex flex-col gap-2\">"
               "<label for=\"activity-type-filter\" class=\"font-semibold text-gray-800 text-sm\">Tipo de Atividade:</label>"
               "<input type=\"text\" id=\"activity-type-filter\" class=\"p-3 border-2 border-gray-300 rounded-md text-base transition-colors focus:outline-none focus:border-indigo-500\" placeholder=\"Filtrar por tipo\" value=\"" (:activity-type filters) "\" />"
               "</div>"
               "<div class=\"flex flex-col gap-2\">"
               "<button id=\"apply-filters-btn\" class=\"p-3 px-6 border-none rounded-md text-base font-semibold cursor-pointer transition-all bg-gradient-to-r from-indigo-500 to-purple-600 text-white hover:-translate-y-0.5 hover:shadow-lg active:translate-y-0\">Aplicar Filtros</button>"
               "</div>"))
    (let [date-input (.querySelector container "#date-filter")
          activity-input (.querySelector container "#activity-filter")
          activity-type-input (.querySelector container "#activity-type-filter")
          apply-btn (.querySelector container "#apply-filters-btn")]
      (.addEventListener date-input "change" #(handle-filter-change :date %))
      (.addEventListener activity-input "input" #(update-filter! :activity (-> % .-target .-value)))
      (.addEventListener activity-type-input "input" #(update-filter! :activity-type (-> % .-target .-value)))
      (.addEventListener apply-btn "click" handle-apply-filters))
    container))

(defn render-upload
  []
  (let [container (.createElement js/document "div")
        status (:upload-status @app-state)]
    (set! (.-className container) "")
    (set! (.-innerHTML container)
          (str "<div class=\"border-2 border-dashed border-indigo-500 rounded-lg p-10 text-center bg-indigo-50 transition-all cursor-pointer hover:border-purple-600 hover:bg-indigo-100\">"
               "<label for=\"csv-upload\" class=\"flex flex-col items-center gap-4 cursor-pointer\">"
               "<svg width=\"48\" height=\"48\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\" class=\"text-indigo-500\">"
               "<path d=\"M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4\"></path>"
               "<polyline points=\"17 8 12 3 7 8\"></polyline>"
               "<line x1=\"12\" y1=\"3\" x2=\"12\" y2=\"15\"></line>"
               "</svg>"
               "<span class=\"text-xl font-semibold text-gray-800\">Clique para fazer upload de CSV</span>"
               "<small class=\"text-sm text-gray-600\">Arquivos planned ou executed são aceitos</small>"
               "</label>"
               "<input type=\"file\" id=\"csv-upload\" accept=\".csv\" class=\"hidden\" />"
               "</div>"))
    (let [file-input (.querySelector container "#csv-upload")]
      (.addEventListener file-input "change" handle-file-select))
    (when status
      (.appendChild container (upload-status-message status)))
    container))

(defn render-app
  []
  (let [app-el (.getElementById js/document "app")
        container (.createElement js/document "div")]
    (set! (.-className container) "max-w-6xl mx-auto bg-white rounded-xl shadow-2xl overflow-hidden")
    (set! (.-innerHTML container)
          (str "<header class=\"bg-gradient-to-r from-indigo-500 to-purple-600 text-white p-8 text-center\">"
               "<h1 class=\"text-4xl md:text-5xl mb-2.5\">Volis Challenge</h1>"
               "<p class=\"text-lg opacity-90\">Gerenciamento de Atividades Planejadas e Executadas</p>"
               "</header>"
               "<main class=\"p-8 md:p-8\">"
               "<section class=\"mb-10 upload-section\">"
               "<h2 class=\"text-2xl mb-5 text-gray-800\">Upload de Arquivos CSV</h2>"
               "</section>"
               "<section class=\"mb-10 filters-section\">"
               "<h2 class=\"text-2xl mb-5 text-gray-800\">Filtros</h2>"
               "</section>"
               "</main>"))
    (let [upload-section (.querySelector container ".upload-section")
          filters-section (.querySelector container ".filters-section")]
      (.appendChild upload-section (render-upload))
      (.appendChild filters-section (render-filters)))
    (set! (.-innerHTML app-el) "")
    (.appendChild app-el container)))

(defn mount-root
  []
  (when (.getElementById js/document "app")
    (render-app)
    (let [filters (:filters @app-state)]
      (when (empty? (:date filters))
        (update-filter! :date (today-date))
        (fetch-activities!)))))

(defn ^:export init
  []
  (mount-root))
