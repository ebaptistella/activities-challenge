(ns challenge.api.adapters.adapters-test
  "Unit tests for adapter functions that transform data between wire formats and models."
  (:require [clojure.test :refer [deftest is testing]]
            [challenge.api.adapters.adapters :as adapters]))

(deftest db-row->activity-test
  (testing "converts database row with non-namespaced keys"
    (let [row {:activity "Test Activity"
               :activity_type "Type A"
               :unit "kg"
               :amount_planned 100M
               :amount_executed 50M}
          result (adapters/db-row->activity row)]
      (is (= "Test Activity" (:activity result)))
      (is (= "Type A" (:activity_type result)))
      (is (= "kg" (:unit result)))
      (is (= 100M (:amount_planned result)))
      (is (= 50M (:amount_executed result)))))

  (testing "converts database row with namespaced keys"
    (let [row {:activity/activity "Test Activity"
               :activity/activity_type "Type B"
               :activity/unit "m"
               :activity/amount_planned 200M
               :activity/amount_executed 75M}
          result (adapters/db-row->activity row)]
      (is (= "Test Activity" (:activity result)))
      (is (= "Type B" (:activity_type result)))
      (is (= "m" (:unit result)))
      (is (= 200M (:amount_planned result)))
      (is (= 75M (:amount_executed result)))))

  (testing "prefers non-namespaced keys over namespaced keys"
    (let [row {:activity "Non-namespaced"
               :activity/activity "Namespaced"
               :activity_type "Type C"
               :activity/activity_type "Type D"}
          result (adapters/db-row->activity row)]
      (is (= "Non-namespaced" (:activity result)))
      (is (= "Type C" (:activity_type result)))))

  (testing "handles nil values"
    (let [row {:activity nil
               :activity_type nil
               :unit nil
               :amount_planned nil
               :amount_executed nil}
          result (adapters/db-row->activity row)]
      (is (nil? (:activity result)))
      (is (nil? (:activity_type result)))
      (is (nil? (:unit result)))
      (is (nil? (:amount_planned result)))
      (is (nil? (:amount_executed result)))))

  (testing "handles missing keys"
    (let [row {:activity "Test"}
          result (adapters/db-row->activity row)]
      (is (= "Test" (:activity result)))
      (is (nil? (:activity_type result)))
      (is (nil? (:unit result)))
      (is (nil? (:amount_planned result)))
      (is (nil? (:amount_executed result))))))

(deftest csv-row->activity-test
  (testing "returns csv activity as-is"
    (let [csv-activity {:date "2024-01-15"
                        :activity "Test Activity"
                        :activity_type "Type A"
                        :unit "kg"
                        :amount 100M}
          result (adapters/csv-row->activity csv-activity)]
      (is (= csv-activity result))
      (is (= "2024-01-15" (:date result)))
      (is (= "Test Activity" (:activity result)))
      (is (= "Type A" (:activity_type result)))
      (is (= "kg" (:unit result)))
      (is (= 100M (:amount result)))))

  (testing "preserves all fields from CSV"
    (let [csv-activity {:date "2024-12-31"
                        :activity "Another Activity"
                        :activity_type "Type B"
                        :unit "m"
                        :amount 200.5M
                        :extra-field "extra"}
          result (adapters/csv-row->activity csv-activity)]
      (is (= "2024-12-31" (:date result)))
      (is (= "Another Activity" (:activity result)))
      (is (= "Type B" (:activity_type result)))
      (is (= "m" (:unit result)))
      (is (= 200.5M (:amount result)))
      (is (= "extra" (:extra-field result))))))

(deftest wire->query-filters-test
  (testing "converts wire query parameters to domain format"
    (let [query-params {"date" "2024-01-15"
                        "activity" "Test Activity"
                        "activity_type" "Type A"
                        "type" "planned"}
          result (adapters/wire->query-filters query-params)]
      (is (= "2024-01-15" (:date result)))
      (is (= "Test Activity" (:activity result)))
      (is (= "Type A" (:activity_type result)))
      (is (= "planned" (:type result)))))

  (testing "handles missing parameters"
    (let [query-params {"date" "2024-01-15"}
          result (adapters/wire->query-filters query-params)]
      (is (= "2024-01-15" (:date result)))
      (is (nil? (:activity result)))
      (is (nil? (:activity_type result)))
      (is (nil? (:type result)))))

  (testing "handles empty query params"
    (let [query-params {}
          result (adapters/wire->query-filters query-params)]
      (is (nil? (:date result)))
      (is (nil? (:activity result)))
      (is (nil? (:activity_type result)))
      (is (nil? (:type result)))))

  (testing "handles nil values in query params"
    (let [query-params {"date" nil
                        "activity" nil
                        "activity_type" nil
                        "type" nil}
          result (adapters/wire->query-filters query-params)]
      (is (nil? (:date result)))
      (is (nil? (:activity result)))
      (is (nil? (:activity_type result)))
      (is (nil? (:type result))))))

(deftest model->wire-response-test
  (testing "returns model as-is for wire response"
    (let [model {:activity "Test Activity"
                 :activity_type "Type A"
                 :unit "kg"
                 :amount 100M
                 :kind "executed"}
          result (adapters/model->wire-response model)]
      (is (= model result))
      (is (= "Test Activity" (:activity result)))
      (is (= "Type A" (:activity_type result)))
      (is (= "kg" (:unit result)))
      (is (= 100M (:amount result)))
      (is (= "executed" (:kind result)))))

  (testing "preserves all fields from model"
    (let [model {:activity "Another Activity"
                 :activity_type "Type B"
                 :unit "m"
                 :amount 200M
                 :kind "planned"
                 :extra-field "extra"}
          result (adapters/model->wire-response model)]
      (is (= "Another Activity" (:activity result)))
      (is (= "Type B" (:activity_type result)))
      (is (= "m" (:unit result)))
      (is (= 200M (:amount result)))
      (is (= "planned" (:kind result)))
      (is (= "extra" (:extra-field result)))))

  (testing "handles empty model"
    (let [model {}
          result (adapters/model->wire-response model)]
      (is (= {} result))
      (is (empty? result)))))
