(ns challenge.handlers.routes.import
  (:require [challenge.infrastructure.http-server.import :as http-server.import]
            [challenge.interceptors.import :as interceptors.import]
            [challenge.wire.out.error :as wire.out.error]
            [challenge.wire.out.import :as wire.out.import]))

(def routes
  #{["/api/v1/import"
     :post
     [interceptors.import/multipart-params
      http-server.import/import-handler]
     :route-name :import-csv
     :summary "Import activities from CSV"
     :doc "Uploads a CSV file (multipart/form-data, field 'file') to import activities (planned or executed)."
     :request-body {:required true
                    :content {"multipart/form-data" {:schema {:type "object"
                                                              :properties {"file" {:type "string" :format "binary"}}
                                                              :required ["file"]}}}}
     :responses {200 {:body wire.out.import/ImportResponse
                      :description "Import completed with type, valid and invalid counts"}
                 400 {:body wire.out.error/ErrorResponse
                      :description "No file or invalid request"}}]})
