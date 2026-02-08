(ns challenge.handlers.routes.swagger
  (:require [challenge.infrastructure.http-server.swagger :as http-server.swagger]
            [schema.core :as s]))

(s/defn create-swagger-routes
  [all-routes-with-docs]
  (let [swagger-json-handler (http-server.swagger/create-swagger-json-handler all-routes-with-docs)]
    #{["/swagger.json"
       :get
       swagger-json-handler
       :route-name :swagger-json]

      ["/swagger-ui"
       :get
       http-server.swagger/swagger-ui-handler
       :route-name :swagger-ui]

      ["/swagger-ui/"
       :get
       http-server.swagger/swagger-ui-handler
       :route-name :swagger-ui-slash]}))
