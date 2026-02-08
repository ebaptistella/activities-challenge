(ns challenge.infrastructure.http-server.import
  (:require [challenge.controllers.import :as controllers.import]
            [challenge.interface.http.response :as response]
            [clojure.string :as string]))

(defn- file-part->csv-string
  "Reads CSV content from a multipart file part (map with :bytes or :stream)."
  [file-part]
  (cond
    (:bytes file-part)
    (String. ^bytes (:bytes file-part) "UTF-8")
    (:stream file-part)
    (slurp (:stream file-part) :encoding "UTF-8")
    :else
    nil))

(defn- filename->type
  "Infers import type from filename (e.g. *_planned.csv -> \"planned\")."
  [filename]
  (if (and filename (string/includes? (string/lower-case (str filename)) "_planned"))
    "planned"
    "executed"))

(defn import-handler
  "Handles POST /api/v1/import (multipart CSV upload).
   Expects :multipart-params with \"file\" key (set by multipart interceptor).
   Returns 400 if no file; otherwise runs import logic and returns
   {:type \"planned\" :valid N :invalid M}."
  [{:keys [multipart-params] componentes :componentes}]
  (if-let [file-part (get multipart-params "file")]
    (let [csv-content (file-part->csv-string file-part)
          filename (get file-part :filename "")
          type (filename->type filename)
          {:keys [persistency]} componentes]
      (if (or (nil? csv-content) (empty? csv-content))
        (response/bad-request "File content is empty")
        (let [result (controllers.import/import-csv! csv-content type persistency)]
          (response/ok result))))
    (response/bad-request "File required")))
