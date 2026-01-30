(ns challenge.api.config.config
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]))

(defn- parse-int [value]
  (when value
    (Integer/parseInt (str value))))

(defn- getenv [k]
  (System/getenv k))

(defn- load-base-config []
  (with-open [r (io/reader (io/resource "config.edn"))]
    (edn/read (java.io.PushbackReader. r))))

(defn- env-overrides [base]
  (let [db (:database base)
        http (:http base)
        log (:log base)]
    {:database (merge db {:host (or (getenv "DB_HOST") (:host db))
                          :port (or (some-> (getenv "DB_PORT") parse-int) (:port db))
                          :name (or (getenv "DB_NAME") (:name db))
                          :user (or (getenv "DB_USER") (:user db))
                          :password (or (getenv "DB_PASSWORD") (:password db))})
     :http     (merge http {:port (or (some-> (getenv "HTTP_PORT") parse-int) (:port http))})
     :log      (merge log {:level (or (getenv "LOG_LEVEL") (:level log))
                           :format (or (some-> (getenv "LOG_FORMAT") keyword) (:format log))})}))

(defn load-config []
  (env-overrides (load-base-config)))

