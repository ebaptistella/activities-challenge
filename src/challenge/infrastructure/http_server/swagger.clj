(ns challenge.infrastructure.http-server.swagger
  (:require [challenge.infrastructure.http-server.swagger.doc :as swagger.doc]
            [challenge.infrastructure.http-server.swagger.generator :as swagger.generator]
            [challenge.interface.http.response :as response]
            [clojure.java.io :as io]))

(defn create-swagger-json-handler
  [all-routes-with-docs]
  (fn swagger-json-handler [_request]
    (let [route-docs (swagger.doc/extract-route-docs all-routes-with-docs)
          spec (swagger.generator/generate-openapi-spec route-docs)]
      (response/ok spec))))

(defn swagger-ui-handler
  [_request]
  (let [swagger-ui-file (io/resource "public/swagger-ui.html")
        swagger-ui-html (if swagger-ui-file
                          (slurp swagger-ui-file)
                          (throw (ex-info "swagger-ui.html not found in resources/public/"
                                          {:resource-path "public/swagger-ui.html"})))]
    {:status 200
     :headers {"Content-Type" "text/html"
               "Content-Security-Policy" "default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval' https://unpkg.com; style-src 'self' 'unsafe-inline' https://unpkg.com; font-src 'self' https://unpkg.com; img-src 'self' data: https:; connect-src 'self';"}
     :body swagger-ui-html}))
