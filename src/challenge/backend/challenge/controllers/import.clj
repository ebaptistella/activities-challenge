(ns challenge.controllers.import
  (:require [challenge.components.persistency :refer [IPersistencySchema]]
            [challenge.infrastructure.persistency.activity :as persistency.activity]
            [challenge.logic.import :as logic.import]
            [schema.core :as s]))

(s/defn import-csv! :- #{s/Keyword s/Any}
  [csv-string :- s/Str
   type :- s/Str
   persistency :- IPersistencySchema]
  (let [current-date (java.time.LocalDate/now)
        rows (logic.import/parse-csv-rows csv-string type)
        {:keys [valid invalid]} (logic.import/process-import-rows rows current-date)
        saved-count (count valid)]
    (doseq [activity valid]
      (persistency.activity/save! activity persistency))
    {:type (str type)
     :valid saved-count
     :invalid invalid}))
