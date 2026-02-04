(ns challenge.handlers.routes.swagger
  (:require [challenge.infrastructure.http-server.swagger :as http-server.swagger]))

(def routes
  #{["/swagger.json"
     :get
     http-server.swagger/swagger-json-handler
     :route-name :swagger-json]

    ["/swagger-ui"
     :get
     http-server.swagger/swagger-ui-handler
     :route-name :swagger-ui]

    ["/swagger-ui/"
     :get
     http-server.swagger/swagger-ui-handler
     :route-name :swagger-ui-slash]})
