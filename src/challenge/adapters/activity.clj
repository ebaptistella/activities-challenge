(ns challenge.adapters.activity
  (:require [challenge.models.activity :as models.activity]
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

(s/defn update-wire->model :- models.activity/Activity
  [{:keys [date activity activity-type unit amount-planned amount-executed]}]
  {:id nil
   :date (when date (LocalDate/parse date))
   :activity activity
   :activity-type activity-type
   :unit unit
   :amount-planned amount-planned
   :amount-executed amount-executed
   :created-at nil
   :updated-at nil})

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
                {:id (:id activity)
                 :date (:date activity)
                 :activity (:activity activity)
                 :activity_type (:activity-type activity)
                 :unit (:unit activity)
                 :amount_planned (:amount-planned activity)
                 :amount_executed (:amount-executed activity)
                 :created_at (:created-at activity)
                 :updated_at (:updated-at activity)})))

(s/defn persistency->model :- models.activity/Activity
  [db-result :- wire.persistency.activity/ActivityPersistency]
  {:id (:id db-result)
   :date (when-let [d (:date db-result)]
           (if (instance? java.sql.Date d)
             (.toLocalDate d)
             (if (string? d)
               (java.time.LocalDate/parse d)
               d)))
   :activity (:activity db-result)
   :activity-type (:activity_type db-result)
   :unit (:unit db-result)
   :amount-planned (:amount_planned db-result)
   :amount-executed (:amount_executed db-result)
   :created-at (when-let [ca (:created_at db-result)]
                 (if (instance? java.sql.Timestamp ca)
                   (.toInstant ca)
                   (if (string? ca)
                     (java.time.Instant/parse ca)
                     ca)))
   :updated-at (when-let [ua (:updated_at db-result)]
                 (if (instance? java.sql.Timestamp ua)
                   (.toInstant ua)
                   (if (string? ua)
                     (java.time.Instant/parse ua)
                     ua)))})