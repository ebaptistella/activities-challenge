(ns challenge.core
  (:gen-class)
  (:require [challenge.config :as config]))

(defn -main
  [& _]
  (let [cfg (config/load-config)]
    cfg))
