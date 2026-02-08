(ns challenge.integration.aux.mock-persistency
  (:require [challenge.components.persistency :as components.persistency]
            [challenge.models.activity :as models.activity]
            [com.stuartsierra.component :as component]
            [schema.core :as s])
  (:import [java.time Instant]))

(defprotocol IMockDatasource
  "Mock datasource protocol for in-memory storage"
  (get-storage [this])
  (clear-storage [this]))

(defrecord MockPersistencyComponent [storage]
  component/Lifecycle
  (start [this]
    (if storage
      this
      (assoc this :storage (atom {}))))
  (stop [this]
    (dissoc this :storage))

  components.persistency/IPersistency
  (get-datasource [this]
    this)

  IMockDatasource
  (get-storage [_this]
    storage)
  (clear-storage [_this]
    (reset! storage {})))

(s/defn new-mock-persistency
  "Creates a new mock persistency component"
  []
  (map->MockPersistencyComponent {}))

;; Mock implementations of persistency functions
(s/defn find-by-id :- (s/maybe models.activity/Activity)
  [activity-id :- s/Int
   persistency]
  (let [storage (get-storage persistency)
        activities @storage]
    (get activities activity-id)))

(s/defn find-all :- [models.activity/Activity]
  ([persistency]
   (find-all persistency nil))
  ([persistency _filters]
   (let [storage (get-storage persistency)
         activities @storage]
     (->> activities
          vals
          (sort-by (juxt :date :id) #(compare %2 %1))))))

(s/defn save! :- models.activity/Activity
  [activity :- models.activity/Activity
   persistency]
  (let [storage (get-storage persistency)
        now (Instant/now)
        activity-with-timestamps (cond-> activity
                                   (nil? (:id activity))
                                   (assoc :id (or (:id activity)
                                                  (inc (apply max 0 (keys @storage))))
                                          :created-at now
                                          :updated-at now)
                                   (:id activity)
                                   (assoc :updated-at now))
        activity-id (:id activity-with-timestamps)]
    (swap! storage assoc activity-id activity-with-timestamps)
    activity-with-timestamps))

(s/defn delete! :- s/Bool
  [activity-id :- s/Int
   persistency]
  (let [storage (get-storage persistency)
        exists? (contains? @storage activity-id)]
    (when exists?
      (swap! storage dissoc activity-id))
    exists?))
