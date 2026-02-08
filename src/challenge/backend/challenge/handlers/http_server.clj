(ns challenge.handlers.http-server
  (:require [challenge.handlers.routes.activity :as routes.activity]
            [challenge.handlers.routes.health :as routes.health]
            [challenge.handlers.routes.import :as routes.import]
            [challenge.handlers.routes.static :as routes.static]
            [challenge.handlers.routes.swagger :as routes.swagger]
            [challenge.infrastructure.http-server.swagger.doc :as swagger.doc]
            [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [schema.core :as s])
  (:import [javax.servlet MultipartConfigElement]
           [org.eclipse.jetty.servlet ServletContextHandler]))

(s/defn ^:private  combine-routes
  []
  (set (concat routes.health/routes
               routes.activity/routes
               routes.import/routes
               routes.static/routes)))

(def all-routes-with-docs
  (let [api-routes (combine-routes)
        swagger-routes (routes.swagger/create-swagger-routes api-routes)]
    (concat api-routes swagger-routes)))

(def routes
  (route/expand-routes (swagger.doc/clean-routes-for-pedestal all-routes-with-docs)))

(def ^:private multipart-config
  "MultipartConfigElement: location, maxFileSize (10MB), maxRequestSize (10MB), fileSizeThreshold (0)."
  (MultipartConfigElement. "" (long 10485760) (long 10485760) 0))

(defn- context-configurator
  "Configura multipart nos servlets do Jetty para que POST /api/v1/import aceite file upload."
  [^ServletContextHandler context]
  (let [servlet-handler (.getServletHandler context)
        holders         (.getServlets servlet-handler)]
    (doseq [holder holders]
      (when-let [reg (.getRegistration holder)]
        (.setMultipartConfig reg multipart-config)))))

(def server-config
  (merge {::http/type :jetty
          ::http/routes routes
          ::http/resource-path "/public"
          ;; CSP that allows static script tags: 'self' (app.js) and Tailwind CDN.
          ;; Pedestal default uses 'strict-dynamic', which blocks scripts not loaded by a trusted script.
          ::http/secure-headers {:content-security-policy-settings
                                 (str "object-src 'none'; "
                                      "script-src 'self' 'unsafe-inline' 'unsafe-eval' https://cdn.tailwindcss.com;")}
          ::http/container-options {:context-configurator context-configurator}}
         {::http/join? false}))