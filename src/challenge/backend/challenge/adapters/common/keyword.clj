(ns challenge.adapters.common.keyword
  (:require [challenge.adapters.common.string :as string]
            [schema.core :as s]))

(s/defn keyword->db-column
  [k]
  (keyword (string/kebab->snake (name k))))

(s/defn convert-keys-to-db-format
  [persistency-wire]
  (into {} (map (fn [[k v]]
                  [(keyword->db-column k) v])
                persistency-wire)))
