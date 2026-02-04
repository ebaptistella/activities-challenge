(ns challenge.infrastructure.http-server.swagger.doc)

(defn- parse-route
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

(defn- remove-doc-keys
  [route]
  (let [{:keys [path method handler-or-interceptors route-map]} (parse-route route)
        cleaned-map (dissoc route-map :doc :responses :request-body :parameters :summary :description)]
    (vec (concat [path method]
                 handler-or-interceptors
                 (apply concat cleaned-map)))))

(defn extract-route-docs
  [routes]
  (reduce (fn [acc route]
            (let [{:keys [path method route-map]} (parse-route route)
                  route-name (:route-name route-map)]
              (if (and route-name
                       (not= route-name :swagger-json)
                       (not= route-name :swagger-ui)
                       (not= route-name :swagger-ui-slash))
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
          routes))

(defn clean-routes-for-pedestal
  [routes]
  (set (map remove-doc-keys routes)))
