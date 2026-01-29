(ns volis-challenge.api
  (:require
   [clojure.java.io :as io]
   [clojure.data.json :as json]
   [reitit.ring :as reitit.ring]
   [ring.middleware.multipart-params :as multipart]
   [ring.util.response :as response]
   [volis-challenge.csv :as csv]
   [volis-challenge.db :as db]
   [volis-challenge.domain :as domain]))

(defn import-summary
  [parsed]
  (let [{:keys [type rows errors]} parsed
        total (count rows)
        error-count (count errors)]
    {:type type
     :lines_read (+ total error-count)
     :valid total
     :invalid error-count
     :errors errors}))

(defn import-handler
  [ds request]
  (let [file (get-in request [:params "file"])
        tempfile (:tempfile file)]
    (if (nil? tempfile)
      {:status 400
       :headers {"Content-Type" "application/json"}
       :body (json/write-str {:error "Arquivo CSV nao enviado"})}
      (try
        (with-open [r (io/reader tempfile)]
          (let [parsed (csv/parse-csv-reader r)
                {:keys [type rows]} parsed]
            (case type
              :planned (db/import-planned-batch! ds rows)
              :executed (db/import-executed-batch! ds rows))
            {:status 200
             :headers {"Content-Type" "application/json"}
             :body (json/write-str (import-summary parsed))}))
        (catch clojure.lang.ExceptionInfo e
          (let [data (ex-data e)]
            {:status 400
             :headers {"Content-Type" "application/json"}
             :body (json/write-str {:error (.getMessage e)
                                    :details data})}))))))

(defn activities-handler
  [ds request]
  (let [date (get-in request [:query-params "date"])
        activity (get-in request [:query-params "activity"])
        activity-type (get-in request [:query-params "activity_type"])
        type (get-in request [:query-params "type"])]
    (if (nil? date)
      {:status 400
       :headers {"Content-Type" "application/json"}
       :body (json/write-str {:error "Parametro 'date' e obrigatorio"})}
      (let [result (domain/plano-x-realizado ds {:date date
                                                 :activity activity
                                                 :activity_type activity-type
                                                 :type type})]
        {:status 200
         :headers {"Content-Type" "application/json"}
         :body (json/write-str result)}))))

(defn static-file-handler
  [request]
  (let [uri (:uri request)
        resource-path (if (= uri "/")
                        "public/index.html"
                        (str "public" uri))
        resource (response/resource-response resource-path)]
    (or resource
        {:status 404
         :headers {}
         :body "Not found"})))

(defn routes
  [ds]
  [["/health" {:get (fn [_] {:status 200 :headers {} :body "ok"})}]
   ["/" {:get (fn [_] (response/resource-response "public/index.html"))}]
   ["/api/import" {:post (fn [req] (import-handler ds req))}]
   ["/api/activities" {:get (fn [req] (activities-handler ds req))}]])

(defn handler
  [ds]
  (let [router (reitit.ring/router (routes ds))
        reitit-handler (reitit.ring/ring-handler router static-file-handler)]
    (multipart/wrap-multipart-params reitit-handler)))

