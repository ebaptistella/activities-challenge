(ns challenge.adapters.common.string
  (:require [clojure.string :as str]
            [schema.core :as s]))

(s/defn kebab->snake
  [s]
  (str/replace s #"-" "_"))
