(ns challenge.infrastructure.http-server.static
  (:require [clojure.java.io :as io]))

(defn serve-static-file
  [file-path & {:keys [content-type headers]
                :or {content-type "text/html"}}]
  (fn [_request]
    (let [resource-file (io/resource (str "public/" file-path))
          file-content (if resource-file
                         (slurp resource-file)
                         (throw (ex-info (str file-path " not found in resources/public/")
                                         {:resource-path (str "public/" file-path)})))
          default-headers {"Content-Type" content-type}
          merged-headers (merge default-headers headers)]
      {:status 200
       :headers merged-headers
       :body file-content})))
