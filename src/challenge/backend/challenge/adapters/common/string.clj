(ns challenge.adapters.common.string
  (:require [clojure.string :as str]
            [schema.core :as s]))

(s/defn kebab->snake :- s/Str
  [s :- s/Str]
  (str/replace s #"-" "_"))
