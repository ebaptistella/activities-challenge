(ns challenge.infrastructure.http-server.swagger.doc
  (:require [schema.core :as s]))

(s/defn ^:private  parse-route
  [route]
  (let [route-vec (vec route)
        path (first route-vec)
        method (second route-vec)
        rest (drop 2 route-vec)
        keyword-args-start (first (keep-indexed
                                   (fn [i x]
                                     (when (keyword? x) i))
                                   rest))
        handler-or-interceptors (take keyword-args-start rest)
        keyword-args (drop keyword-args-start rest)
        route-map (apply hash-map keyword-args)]
    {:path path
     :method method
     :handler-or-interceptors handler-or-interceptors
     :route-map route-map}))

(s/defn ^:private  remove-doc-keys
  [route]
  (let [{:keys [path method handler-or-interceptors route-map]} (parse-route route)
        cleaned-map (dissoc route-map :doc :responses :request-body :parameters :summary :description)]
    (vec (concat [path method]
                 handler-or-interceptors
                 (apply concat cleaned-map)))))

(s/defn extract-route-docs
  [routes]
  (let [excluded-routes #{:swagger-json
                          :swagger-ui
                          :swagger-ui-slash
                          :home}]
    (reduce (fn [acc route]
              (let [{:keys [path method route-map]} (parse-route route)
                    route-name (:route-name route-map)]
                (if (and route-name
                         (not (contains? excluded-routes route-name)))
                  (assoc acc route-name
                         {:path path
                          :method method
                          :summary (:summary route-map)
                          :description (:doc route-map)
                          :responses (:responses route-map)
                          :request-body (:request-body route-map)
                          :parameters (:parameters route-map)})
                  acc)))
            {}
            routes)))

(s/defn clean-routes-for-pedestal
  [routes]
  (set (map remove-doc-keys routes)))
