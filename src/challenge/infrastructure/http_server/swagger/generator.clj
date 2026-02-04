(ns challenge.infrastructure.http-server.swagger.generator
  (:require [clojure.string :as string]))

(defn- extract-key-name
  [k]
  (cond
    (instance? schema.core.OptionalKey k)
    (name (.k ^schema.core.OptionalKey k))
    (keyword? k)
    (name k)
    (string? k)
    k
    :else
    (str k)))

(defn- schema-to-json-schema
  [schema]
  (cond
    ;; Schemas do Prismatic Schema são classes Java
    (= schema java.lang.String) {:type "string"}
    (= schema java.lang.Integer) {:type "integer"}
    (= schema java.lang.Long) {:type "integer"}
    (= schema java.lang.Double) {:type "number"}
    (= schema java.lang.Float) {:type "number"}
    (= schema java.lang.Boolean) {:type "boolean"}
    ;; Maybe schemas
    (and (list? schema) (= (first schema) 'maybe))
    (assoc (schema-to-json-schema (second schema)) :nullable true)
    ;; Arrays
    (and (vector? schema) (= 1 (count schema)))
    {:type "array"
     :items (schema-to-json-schema (first schema))}
    ;; Mapas/objetos - assumimos que é um schema do Prismatic
    (map? schema)
    (let [properties (reduce-kv
                      (fn [acc k v]
                        (let [key-name (extract-key-name k)
                              prop-schema (schema-to-json-schema v)]
                          (assoc acc key-name prop-schema)))
                      {}
                      schema)
          required (vec (keep (fn [[k _v]]
                                (when-not (instance? schema.core.OptionalKey k)
                                  (extract-key-name k)))
                              schema))]
      (cond-> {:type "object"
               :properties properties}
        (seq required) (assoc :required required)))
    ;; Default: objeto genérico
    :else {:type "object" :description "Schema type not fully specified"}))

(defn- response-schema-to-openapi
  [response-schema]
  (if (map? response-schema)
    (let [body-schema (:body response-schema)
          description (:description response-schema)]
      {:description (or description "")
       :content {"application/json" {:schema (if body-schema
                                               (schema-to-json-schema body-schema)
                                               {:type "object"})}}})
    {:description ""
     :content {"application/json" {:schema {:type "object"}}}}))

(defn- path-param-to-openapi
  [param]
  (if (string? param)
    {:name param
     :in "path"
     :required true
     :schema {:type "string"}}
    param))

(defn- extract-path-params
  [path]
  (let [matches (re-seq #":(\w+)" path)]
    (map (fn [[_ param-name]]
           {:name param-name
            :in "path"
            :required true
            :schema {:type "integer"}})
         matches)))

(defn- request-body-to-openapi
  [request-body]
  (if (map? request-body)
    (let [content (:content request-body)
          required (:required request-body)]
      (cond-> {:required (or required true)}
        content (assoc :content
                       (reduce-kv
                        (fn [acc content-type schema-map]
                          (let [schema (:schema schema-map)]
                            (assoc acc content-type
                                   {:schema (schema-to-json-schema schema)})))
                        {}
                        content))))
    request-body))

(defn- route-doc-to-openapi-path
  [route-doc]
  (let [method (name (:method route-doc))
        summary (:summary route-doc)
        description (:description route-doc)
        responses (:responses route-doc)
        request-body (:request-body route-doc)
        parameters (or (:parameters route-doc)
                       (extract-path-params (:path route-doc)))]
    {(keyword method)
     (cond-> {}
       summary (assoc :summary summary)
       description (assoc :description description)
       (seq parameters) (assoc :parameters (map path-param-to-openapi parameters))
       request-body (assoc :requestBody (request-body-to-openapi request-body))
       (seq responses) (assoc :responses
                              (reduce-kv
                               (fn [acc status-code response-schema]
                                 (assoc acc (str status-code) (response-schema-to-openapi response-schema)))
                               {}
                               responses)))}))

(defn generate-openapi-spec
  [route-docs]
  (let [paths (reduce-kv
               (fn [acc _route-name route-doc]
                 (let [path (:path route-doc)
                       openapi-path (route-doc-to-openapi-path route-doc)
                       ;; Converte :id para {id} para OpenAPI
                       openapi-path-str (clojure.string/replace path #":(\w+)" "{$1}")]
                   (update acc openapi-path-str merge openapi-path)))
               {}
               route-docs)]
    {:openapi "3.0.0"
     :info {:title "Challenge API"
            :version "1.0.0"
            :description "API for managing activities"}
     :paths paths}))
