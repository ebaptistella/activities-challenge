(ns challenge.interceptors.import
  "Interceptors for CSV import (multipart parsing via Servlet API)."
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [io.pedestal.interceptor :as interceptor]))

(defn- part->bytes
  [part]
  (with-open [in (.getInputStream part)]
    (let [out (java.io.ByteArrayOutputStream.)]
      (io/copy in out)
      (.toByteArray out))))

(def multipart-params
  "Interceptor that reads the multipart 'file' part from the servlet request
   and adds :multipart-params {\"file\" {:filename _ :bytes _}} to the request.
   Only intended for POST /api/v1/import route. Requires :servlet-request on request."
  (interceptor/interceptor
   {:name ::multipart-params
    :enter (fn [context]
             (let [request (:request context)
                   servlet-request (:servlet-request request)
                   content-type (some #(get-in request [:headers %])
                                     ["content-type" "Content-Type"])
                   s (str (or content-type ""))]
               (if (and servlet-request
                        (string/includes? (string/lower-case s) "multipart/form-data"))
                 (try
                   (if-let [part (.getPart servlet-request "file")]
                     (let [filename (or (.getSubmittedFileName part) (.getName part) "upload.csv")
                           bytes (part->bytes part)
                           params {"file" {:filename filename :bytes bytes}}]
                       (-> context
                           (assoc-in [:request :multipart-params] params)
                           (assoc-in [:request :params] params)))
                     (assoc context
                            :response {:status 400
                                       :headers {"Content-Type" "application/json"}
                                       :body "{\"error\":\"File part 'file' required\"}"}))
                 (catch Exception e
                   (assoc context
                          :response {:status 400
                                     :headers {"Content-Type" "application/json"}
                                     :body (str "{\"error\":\"Invalid multipart: "
                                                (.getMessage e) "\"")})))
                 context)))}))
