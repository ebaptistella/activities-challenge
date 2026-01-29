(ns volis-challenge.api
  (:require
   [clojure.java.io :as io]
   [reitit.ring :as reitit.ring]
   [ring.middleware.multipart-params :as multipart]
   [ring.util.response :as response]
   [volis-challenge.csv :as csv]
   [volis-challenge.db :as db]))

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
       :headers {}
       :body {:error "Arquivo CSV nao enviado"}}
      (try
        (with-open [r (io/reader tempfile)]
          (let [parsed (csv/parse-csv-reader r)
                {:keys [type rows]} parsed]
            (case type
              :planned (db/import-planned-batch! ds rows)
              :executed (db/import-executed-batch! ds rows))
            {:status 200
             :headers {}
             :body (import-summary parsed)}))
        (catch clojure.lang.ExceptionInfo e
          (let [data (ex-data e)]
            {:status 400
             :headers {}
             :body {:error (.getMessage e)
                    :details data}}))))))

(defn activities-handler
  [ds request]
  (let [date (get-in request [:query-params "date"])
        activity (get-in request [:query-params "activity"])
        type (get-in request [:query-params "type"])]
    (if (nil? date)
      {:status 400
       :headers {}
       :body {:error "Parametro 'date' e obrigatorio"}}
      (let [result (db/activities-by-date ds {:date date
                                              :activity activity
                                              :type type})]
        {:status 200
         :headers {}
         :body result}))))

(defn routes
  [ds]
  [["/health" {:get (fn [_] {:status 200 :headers {} :body "ok"})}]
   ["/" {:get (fn [_] (response/resource-response "public/index.html"))}]
   ["/api/import" {:post (fn [req] (import-handler ds req))}]
   ["/api/activities" {:get (fn [req] (activities-handler ds req))}]])

(defn handler
  [ds]
  (let [router (reitit.ring/router (routes ds))
        app (reitit.ring/ring-handler
             router
             (reitit.ring/create-resource-handler {:path "/"}))]
    (multipart/wrap-multipart-params app)))

