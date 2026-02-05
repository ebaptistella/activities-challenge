(ns challenge.adapters.activity
  (:require [challenge.adapters.common.date :as date]
            [challenge.adapters.common.keyword :as keyword]
            [challenge.models.activity :as models.activity]
            [challenge.wire.in.activity :as wire.in.activity]
            [challenge.wire.out.activity :as wire.out.activity]
            [challenge.wire.persistency.activity :as wire.persistency.activity]
            [schema.core :as s])
  (:import [java.time LocalDate]))

(s/defn wire->model :- models.activity/Activity
  [{:keys [date activity activity-type unit amount-planned amount-executed]}
   :- wire.in.activity/ActivityRequest]
  {:id nil
   :date (when date (LocalDate/parse date))
   :activity activity
   :activity-type activity-type
   :unit unit
   :amount-planned amount-planned
   :amount-executed amount-executed
   :created-at nil
   :updated-at nil})

(s/defn update-wire->model
  [{:keys [date activity activity-type unit amount-planned amount-executed]}]
  (into {}
        (remove (comp nil? second)
                {:date (when date (LocalDate/parse date))
                 :activity activity
                 :activity-type activity-type
                 :unit unit
                 :amount-planned amount-planned
                 :amount-executed amount-executed})))

(s/defn model->wire :- wire.out.activity/ActivityResponse
  [activity :- models.activity/Activity]
  {:id (:id activity)
   :date (when-let [d (:date activity)] (str d))
   :activity (:activity activity)
   :activity-type (:activity-type activity)
   :unit (:unit activity)
   :amount-planned (:amount-planned activity)
   :amount-executed (:amount-executed activity)
   :created-at (when-let [ca (:created-at activity)] (str ca))
   :updated-at (when-let [ua (:updated-at activity)] (str ua))})

(s/defn model->persistency :- wire.persistency.activity/ActivityPersistency
  [activity :- models.activity/Activity]
  (into {}
        (remove (comp nil? second)
                {:activity/id (:id activity)
                 :activity/date (:date activity)
                 :activity/activity (:activity activity)
                 :activity/activity-type (:activity-type activity)
                 :activity/unit (:unit activity)
                 :activity/amount-planned (:amount-planned activity)
                 :activity/amount-executed (:amount-executed activity)
                 :activity/created-at (:created-at activity)
                 :activity/updated-at (:updated-at activity)})))

(defn model->db
  [activity]
  (let [persistency-wire (model->persistency activity)
        with-timestamps (date/convert-instants-to-timestamps persistency-wire)
        db-data (keyword/convert-keys-to-db-format with-timestamps)]
    db-data))

(s/defn persistency->model :- models.activity/Activity
  [db-result]
  (let [id (or (:activity/id db-result) (:id db-result) (get db-result "id"))
        date (or (:activity/date db-result) (:date db-result) (get db-result "date"))
        activity (or (:activity/activity db-result) (:activity db-result) (get db-result "activity"))
        activity-type (or (:activity/activity-type db-result) (:activity/activity_type db-result) (:activity_type db-result) (get db-result "activity_type"))
        unit (or (:activity/unit db-result) (:unit db-result) (get db-result "unit"))
        amount-planned (or (:activity/amount-planned db-result) (:activity/amount_planned db-result) (:amount_planned db-result) (get db-result "amount_planned"))
        amount-executed (or (:activity/amount-executed db-result) (:activity/amount_executed db-result) (:amount_executed db-result) (get db-result "amount_executed"))
        created-at (or (:activity/created-at db-result) (:activity/created_at db-result) (:created_at db-result) (get db-result "created_at"))
        updated-at (or (:activity/updated-at db-result) (:activity/updated_at db-result) (:updated_at db-result) (get db-result "updated_at"))]
    {:id id
     :date (when-let [d date]
             (if (instance? java.sql.Date d)
               (.toLocalDate d)
               (if (string? d)
                 (java.time.LocalDate/parse d)
                 d)))
     :activity activity
     :activity-type activity-type
     :unit unit
     :amount-planned amount-planned
     :amount-executed amount-executed
     :created-at (when-let [ca created-at]
                   (if (instance? java.sql.Timestamp ca)
                     (.toInstant ca)
                     (if (string? ca)
                       (java.time.Instant/parse ca)
                       ca)))
     :updated-at (when-let [ua updated-at]
                   (if (instance? java.sql.Timestamp ua)
                     (.toInstant ua)
                     (if (string? ua)
                       (java.time.Instant/parse ua)
                       ua)))}))