(ns challenge.handlers.routes.static
  (:require [challenge.infrastructure.http-server.static :as static]))

(def routes
  #{["/" :get (static/serve-static-file "index.html") :route-name :home]
    ["/js/*path" :get (fn [request]
                        (let [path (get-in request [:path-params :path])
                              file-path (str "js/" path)]
                          (static/serve-static-file file-path
                                                    :content-type (cond
                                                                    (.endsWith path ".js") "application/javascript"
                                                                    (.endsWith path ".map") "application/json"
                                                                    :else "application/octet-stream"))))
     :route-name :static-js]})
