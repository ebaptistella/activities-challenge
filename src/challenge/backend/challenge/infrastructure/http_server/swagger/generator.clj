(ns challenge.infrastructure.http-server.swagger.generator
  (:require [clojure.string :as string]
            [schema.core :as s]))

(s/defn ^:private  extract-key-name
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

(s/defn ^:private  prismatic-type-to-json-type
  [schema]
  (cond
    (= schema s/Str) {:type "string"}
    (= schema s/Int) {:type "integer"}
    (= schema s/Num) {:type "number"}
    (= schema s/Bool) {:type "boolean"}
    (= schema java.lang.String) {:type "string"}
    (= schema java.lang.Integer) {:type "integer"}
    (= schema java.lang.Long) {:type "integer"}
    (= schema java.lang.Double) {:type "number"}
    (= schema java.lang.Float) {:type "number"}
    (= schema java.lang.Boolean) {:type "boolean"}
    :else nil))

(s/defn ^:private  is-maybe-schema?
  [schema]
  (try
    (or
     (and (seq? schema)
          (>= (count schema) 2)
          (= (first schema) s/maybe))
     (instance? schema.core.Maybe schema))
    (catch Exception _ false)))

(s/defn ^:private  extract-maybe-schema
  [schema]
  (try
    (cond
      (and (seq? schema) (= (first schema) s/maybe))
      (second schema)
      (instance? schema.core.Maybe schema)
      (.schema ^schema.core.Maybe schema)
      :else nil)
    (catch Exception _ nil)))

(s/defn ^:private  schema-to-json-schema
  [schema]
  (let [json-type-result (prismatic-type-to-json-type schema)]
    (cond
      (is-maybe-schema? schema)
      (let [inner-schema (extract-maybe-schema schema)]
        (if inner-schema
          (let [inner-json (schema-to-json-schema inner-schema)]
            (-> inner-json
                (dissoc :example)
                (assoc :nullable true)))
          {:type "object" :nullable true}))
      (some? json-type-result)
      json-type-result
      (and (vector? schema) (= 1 (count schema)))
      {:type "array"
       :items (schema-to-json-schema (first schema))}
      (map? schema)
      (let [is-serialized-schema (and (= 1 (count schema))
                                      (contains? schema :schema)
                                      (not (contains? schema :type)))]
        (if is-serialized-schema
          (schema-to-json-schema (:schema schema))
          (let [properties (reduce-kv
                            (fn [acc k v]
                              (let [key-name (extract-key-name k)
                                    prop-schema (schema-to-json-schema v)
                                    prop-with-example (cond-> prop-schema
                                                        (and (= key-name "date")
                                                             (= (:type prop-schema) "string"))
                                                        (assoc :example "2024-01-15")
                                                        (and (= key-name "activity")
                                                             (= (:type prop-schema) "string"))
                                                        (assoc :example "Example activity")
                                                        (and (= key-name "activity-type")
                                                             (= (:type prop-schema) "string"))
                                                        (assoc :example "type1")
                                                        (and (= key-name "unit")
                                                             (= (:type prop-schema) "string"))
                                                        (assoc :example "kg")
                                                        (and (contains? #{"amount-planned" "amount-executed"} key-name)
                                                             (= (:type prop-schema) "number")
                                                             (not (:nullable prop-schema)))
                                                        (assoc :example 0.0)
                                                        (and (contains? #{"amount-planned" "amount-executed"} key-name)
                                                             (:nullable prop-schema))
                                                        (dissoc :example))]
                                (assoc acc key-name prop-with-example)))
                            {}
                            schema)
                required (vec (keep (fn [[k _v]]
                                      (when-not (instance? schema.core.OptionalKey k)
                                        (extract-key-name k)))
                                    schema))]
            (cond-> {:type "object"
                     :properties properties}
              (seq required) (assoc :required required)))))
      :else {:type "object" :description "Schema type not fully specified"})))

(s/defn ^:private  response-schema-to-openapi
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

(s/defn ^:private  path-param-to-openapi
  [param]
  (if (string? param)
    {:name param
     :in "path"
     :required true
     :schema {:type "string"}}
    param))

(s/defn ^:private  extract-path-params
  [path]
  (let [matches (re-seq #":(\w+)" path)]
    (map (fn [[_ param-name]]
           {:name param-name
            :in "path"
            :required true
            :schema {:type "integer"}})
         matches)))

(s/defn ^:private  request-body-to-openapi
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

(s/defn ^:private  route-doc-to-openapi-path
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

(s/defn generate-openapi-spec
  [route-docs]
  (let [paths (reduce-kv
               (fn [acc _route-name route-doc]
                 (let [path (:path route-doc)
                       openapi-path (route-doc-to-openapi-path route-doc)
                       openapi-path-str (clojure.string/replace path #":(\w+)" "{$1}")]
                   (update acc openapi-path-str merge openapi-path)))
               {}
               route-docs)]
    {:openapi "3.0.0"
     :info {:title "Challenge API"
            :version "1.0.0"
            :description "API for managing activities"}
     :paths paths}))
