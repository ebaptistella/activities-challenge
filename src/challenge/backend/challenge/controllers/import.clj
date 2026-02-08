(ns challenge.controllers.import
  (:require [challenge.infrastructure.persistency.activity :as persistency.activity]
            [challenge.logic.import :as logic.import]
            [schema.core :as s]))

(s/defn import-csv!
  "Imports activities from a CSV string. Parses rows, validates each, saves valid
   ones to persistency. Returns {:type type :valid N :invalid M}."
  [csv-string type persistency]
  (let [current-date (java.time.LocalDate/now)
        rows (logic.import/parse-csv-rows csv-string type)
        {:keys [valid invalid]} (logic.import/process-import-rows rows current-date)
        saved-count (count valid)]
    (doseq [activity valid]
      (persistency.activity/save! activity persistency))
    {:type (str type)
     :valid saved-count
     :invalid invalid}))
