(ns challenge.core
  (:gen-class))

(defn -main
  [& _]
  (let [database-url (System/getenv "DATABASE_URL")]
    (when (nil? database-url)
      (throw (ex-info "DATABASE_URL not configured" {})))))
