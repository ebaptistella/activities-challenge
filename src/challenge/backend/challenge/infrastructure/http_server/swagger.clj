(ns challenge.infrastructure.http-server.swagger
  (:require [challenge.infrastructure.http-server.static :as static]
            [challenge.infrastructure.http-server.swagger.doc :as swagger.doc]
            [challenge.infrastructure.http-server.swagger.generator :as swagger.generator]
            [challenge.interface.http.response :as response]))

(defn create-swagger-json-handler
  [all-routes-with-docs]
  (fn swagger-json-handler [_request]
    (let [route-docs (swagger.doc/extract-route-docs all-routes-with-docs)
          spec (swagger.generator/generate-openapi-spec route-docs)]
      (response/ok spec))))

(def swagger-ui-handler
  (static/serve-static-file "swagger-ui.html"
                            :headers {"Content-Security-Policy" "default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval' https://unpkg.com; style-src 'self' 'unsafe-inline' https://unpkg.com; font-src 'self' https://unpkg.com; img-src 'self' data: https:; connect-src 'self';"}))
