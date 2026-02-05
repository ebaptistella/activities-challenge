(ns challenge.adapters.common.string
  (:require [clojure.string :as str]))

(defn kebab->snake
  [s]
  (str/replace s #"-" "_"))
