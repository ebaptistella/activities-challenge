(ns volis-challenge.db
  (:require
   [next.jdbc :as jdbc]
   [next.jdbc.sql :as sql]
   [clojure.string :as str]
   [clojure.tools.logging :as log]))

(def activity-key-columns
  [:date :activity :activity_type :unit])

(defn- now []
  (java.time.OffsetDateTime/now))

(defn- ensure-local-date [v]
  (cond
    (instance? java.time.LocalDate v) v
    (instance? java.sql.Date v) (.toLocalDate ^java.sql.Date v)
    :else (try
            (let [date-str (str/trim (str v))]
              (log/debug "Convertendo data para LocalDate" {:input v :date-str date-str})
              (java.time.LocalDate/parse date-str))
            (catch Exception e
              (log/error e "Erro ao converter data para LocalDate" {:input v :type (type v)})
              (throw (ex-info (str "Data inválida: " v) {:date v :error (.getMessage e)}))))))

(defn- find-activity
  [tx {:keys [date activity activity_type unit]}]
  (log/debug "Buscando atividade" {:date date :activity activity :activity_type activity_type :unit unit})
  (let [params {:date (ensure-local-date date)
                :activity activity
                :activity_type activity_type
                :unit unit}
        result (first (sql/find-by-keys tx :activity params))]
    (log/debug "Resultado da busca de atividade" {:found (some? result)})
    result))

(defn- insert-activity
  [tx {:keys [date] :as row}]
  (log/debug "Inserindo nova atividade" {:date date :activity (:activity row)})
  (let [payload (-> row
                    (assoc :date (ensure-local-date date))
                    (dissoc :amount))
        result (sql/insert! tx :activity (merge payload {:created_at (now)
                                                         :updated_at (now)}))]
    (log/debug "Atividade inserida com sucesso")
    result))

(defn- update-activity-field
  [tx {:keys [date activity activity_type unit] :as row} field]
  (log/debug "Atualizando campo de atividade" {:date date :activity activity :field field :amount (:amount row)})
  (let [payload {:date (ensure-local-date date)
                 :activity activity
                 :activity_type activity_type
                 :unit unit}
        values {field (:amount row)
                :updated_at (now)}
        result (sql/update! tx :activity values payload)]
    (log/debug "Campo de atividade atualizado" {:rows-affected (first result)})
    result))

(defn- upsert-planned-tx!
  [tx row]
  (if (find-activity tx row)
    (update-activity-field tx row :amount_planned)
    (insert-activity tx (assoc row :amount_planned (:amount row)))))

(defn- upsert-executed-tx!
  [tx row]
  (if (find-activity tx row)
    (update-activity-field tx row :amount_executed)
    (insert-activity tx (assoc row :amount_executed (:amount row)))))

(defn upsert-planned!
  [ds row]
  (jdbc/with-transaction [tx ds]
    (upsert-planned-tx! tx row)))

(defn upsert-executed!
  [ds row]
  (jdbc/with-transaction [tx ds]
    (upsert-executed-tx! tx row)))

(defn import-planned-batch!
  [ds rows]
  (let [start-time (System/currentTimeMillis)
        rows-count (count rows)]
    (log/info "Iniciando importação de atividades planejadas" {:rows-count rows-count})
    (try
      (jdbc/with-transaction [tx ds]
        (doseq [row rows]
          (upsert-planned-tx! tx row)))
      (let [duration (- (System/currentTimeMillis) start-time)]
        (log/info "Importação de atividades planejadas concluída" {:rows-count rows-count :duration-ms duration}))
      (catch Exception e
        (let [duration (- (System/currentTimeMillis) start-time)]
          (log/error e "Erro ao importar atividades planejadas" {:rows-count rows-count :duration-ms duration})
          (throw e))))))

(defn import-executed-batch!
  [ds rows]
  (let [start-time (System/currentTimeMillis)
        rows-count (count rows)]
    (log/info "Iniciando importação de atividades executadas" {:rows-count rows-count})
    (try
      (jdbc/with-transaction [tx ds]
        (doseq [row rows]
          (upsert-executed-tx! tx row)))
      (let [duration (- (System/currentTimeMillis) start-time)]
        (log/info "Importação de atividades executadas concluída" {:rows-count rows-count :duration-ms duration}))
      (catch Exception e
        (let [duration (- (System/currentTimeMillis) start-time)]
          (log/error e "Erro ao importar atividades executadas" {:rows-count rows-count :duration-ms duration})
          (throw e))))))

(defn query-activities-raw
  [ds {:keys [date activity activity_type]}]
  (let [start-time (System/currentTimeMillis)
        filters {:date date :activity activity :activity_type activity_type}]
    (log/info "Executando query de atividades" {:filters filters})
    (try
      (let [base-sql (str
                      "select activity, activity_type, unit, "
                      "amount_planned, amount_executed "
                      "from activity "
                      "where date = ?"
                      (when activity " and activity = ?")
                      (when activity_type " and activity_type = ?"))
            query-params (cond-> [(ensure-local-date date)]
                           activity (conj activity)
                           activity_type (conj activity_type))
            rows (jdbc/execute! ds (into [base-sql] query-params))
            duration (- (System/currentTimeMillis) start-time)]
        (log/info "Query de atividades concluída" {:filters filters :rows-count (count rows) :duration-ms duration})
        rows)
      (catch Exception e
        (let [duration (- (System/currentTimeMillis) start-time)]
          (log/error e "Erro ao executar query de atividades" {:filters filters :duration-ms duration})
          (throw e))))))
