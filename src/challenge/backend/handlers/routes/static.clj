(ns challenge.handlers.routes.static
  (:require [challenge.infrastructure.http-server.static :as static]))

(def routes
  #{["/" :get (static/serve-static-file "index.html") :route-name :home]})
