(ns challenge.adapters.activity
  (:require [challenge.models.activity :as models.activity]
            [challenge.wire.in.activity :as wire.in.activity]
            [challenge.wire.out.activity :as wire.out.activity]
            [challenge.wire.persistency.activity :as wire.persistency.activity]
            [schema.core :as s])
  (:import [java.time LocalDate]))

(s/defn wire->model :- models.activity/Activity
  "Converts ActivityRequest wire schema to Activity model.
   Handles string to LocalDate conversion."
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
  "Converts ActivityUpdateRequest wire schema to Activity model.
   Handles optional fields and string to LocalDate conversion."
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
  "Converts Activity model to ActivityResponse wire schema.
   Handles LocalDate and Instant to string conversion."
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
  "Converts Activity model to ActivityPersistency schema."
  [activity :- models.activity/Activity]
  (wire.persistency.activity/model->persistency activity))

(s/defn persistency->model :- models.activity/Activity
  "Converts ActivityPersistency schema to Activity model."
  [persistency-data]
  (wire.persistency.activity/persistency->model persistency-data))
