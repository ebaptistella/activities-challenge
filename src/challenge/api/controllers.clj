(ns challenge.api.controllers
  "Main controllers namespace for orchestrating business operations."
  (:require [challenge.api.controllers.activities :as activities]
            [challenge.api.controllers.import :as import]))

(defn query-activities!
  "Orchestrates activities query.
  
  Parameters:
  - ds: Database datasource
  - request: Ring request map
  
  Returns:
  - Ring response map"
  [ds request]
  (activities/query-activities! ds request))

(defn import-csv!
  "Orchestrates CSV import.
  
  Parameters:
  - ds: Database datasource
  - request: Ring request map
  
  Returns:
  - Ring response map"
  [ds request]
  (import/import-csv! ds request))
