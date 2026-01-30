(ns challenge.api.logic.logic-test
  "Unit tests for pure business logic functions."
  (:require [clojure.test :refer [deftest is testing]]
            [challenge.api.logic.logic :as logic]))

(deftest calculate-kind-test
  (testing "returns :both when both amounts are present"
    (is (= :both (logic/calculate-kind 100M 50M)))
    (is (= :both (logic/calculate-kind 0M 0M)))
    (is (= :both (logic/calculate-kind 1.5M 2.3M))))

  (testing "returns :planned when only planned amount is present"
    (is (= :planned (logic/calculate-kind 100M nil)))
    (is (= :planned (logic/calculate-kind 0M nil)))
    (is (= :planned (logic/calculate-kind 1.5M nil))))

  (testing "returns :executed when only executed amount is present"
    (is (= :executed (logic/calculate-kind nil 50M)))
    (is (= :executed (logic/calculate-kind nil 0M)))
    (is (= :executed (logic/calculate-kind nil 2.3M))))

  (testing "returns nil when both amounts are nil"
    (is (nil? (logic/calculate-kind nil nil)))))

(deftest select-relevant-amount-test
  (testing "returns planned amount when type-filter is 'planned'"
    (is (= 100M (logic/select-relevant-amount 100M 50M "planned")))
    (is (= 0M (logic/select-relevant-amount 0M 50M "planned")))
    (is (nil? (logic/select-relevant-amount nil 50M "planned"))))

  (testing "returns executed amount when type-filter is 'executed'"
    (is (= 50M (logic/select-relevant-amount 100M 50M "executed")))
    (is (= 0M (logic/select-relevant-amount 100M 0M "executed")))
    (is (nil? (logic/select-relevant-amount 100M nil "executed"))))

  (testing "returns executed amount when type-filter is nil and both are present"
    (is (= 50M (logic/select-relevant-amount 100M 50M nil)))
    (is (= 0M (logic/select-relevant-amount 100M 0M nil))))

  (testing "returns planned amount when type-filter is nil and only planned is present"
    (is (= 100M (logic/select-relevant-amount 100M nil nil)))
    (is (= 0M (logic/select-relevant-amount 0M nil nil))))

  (testing "returns nil when both amounts are nil regardless of filter"
    (is (nil? (logic/select-relevant-amount nil nil "planned")))
    (is (nil? (logic/select-relevant-amount nil nil "executed")))
    (is (nil? (logic/select-relevant-amount nil nil nil)))))

(deftest enrich-activity-test
  (testing "enriches activity with both amounts"
    (let [activity-row {:activity "Test Activity"
                        :activity_type "Type A"
                        :unit "kg"
                        :amount_planned 100M
                        :amount_executed 50M}
          result (logic/enrich-activity activity-row nil)]
      (is (= "Test Activity" (:activity result)))
      (is (= "Type A" (:activity_type result)))
      (is (= "kg" (:unit result)))
      (is (= 50M (:amount result)))
      (is (= "both" (:kind result)))))

  (testing "enriches activity with only planned amount"
    (let [activity-row {:activity "Planned Only"
                        :activity_type "Type B"
                        :unit "m"
                        :amount_planned 200M
                        :amount_executed nil}
          result (logic/enrich-activity activity-row nil)]
      (is (= "Planned Only" (:activity result)))
      (is (= 200M (:amount result)))
      (is (= "planned" (:kind result)))))

  (testing "enriches activity with only executed amount"
    (let [activity-row {:activity "Executed Only"
                        :activity_type "Type C"
                        :unit "l"
                        :amount_planned nil
                        :amount_executed 75M}
          result (logic/enrich-activity activity-row nil)]
      (is (= "Executed Only" (:activity result)))
      (is (= 75M (:amount result)))
      (is (= "executed" (:kind result)))))

  (testing "respects type-filter for amount selection"
    (let [activity-row {:activity "Filtered"
                        :activity_type "Type D"
                        :unit "kg"
                        :amount_planned 100M
                        :amount_executed 50M}
          result-planned (logic/enrich-activity activity-row "planned")
          result-executed (logic/enrich-activity activity-row "executed")]
      (is (= 100M (:amount result-planned)))
      (is (= 50M (:amount result-executed)))))

  (testing "returns nil when both amounts are nil"
    (let [activity-row {:activity "No Amounts"
                        :activity_type "Type E"
                        :unit "kg"
                        :amount_planned nil
                        :amount_executed nil}
          result (logic/enrich-activity activity-row nil)]
      (is (nil? result)))))

(deftest filter-activities-by-kind-test
  (testing "filters and enriches activities with valid kinds"
    (let [activities [{:activity "A" :amount_planned 100M :amount_executed 50M}
                      {:activity "B" :amount_planned 200M :amount_executed nil}
                      {:activity "C" :amount_planned nil :amount_executed 75M}
                      {:activity "D" :amount_planned nil :amount_executed nil}]
          result (logic/filter-activities-by-kind activities nil)]
      (is (= 3 (count result)))
      (is (= "both" (:kind (first result))))
      (is (= "planned" (:kind (second result))))
      (is (= "executed" (:kind (nth result 2))))))

  (testing "respects type-filter when filtering"
    (let [activities [{:activity "A" :amount_planned 100M :amount_executed 50M}
                      {:activity "B" :amount_planned 200M :amount_executed nil}]
          result (logic/filter-activities-by-kind activities "planned")]
      (is (= 2 (count result)))
      (is (= 100M (:amount (first result))))
      (is (= 200M (:amount (second result))))))

  (testing "returns empty vector when all activities have no amounts"
    (let [activities [{:activity "A" :amount_planned nil :amount_executed nil}
                      {:activity "B" :amount_planned nil :amount_executed nil}]
          result (logic/filter-activities-by-kind activities nil)]
      (is (empty? result)))))

(deftest validate-date-parameter-test
  (testing "returns string when date is valid"
    (is (= "2024-01-15" (logic/validate-date-parameter "2024-01-15")))
    (is (= "2024-12-31" (logic/validate-date-parameter "2024-12-31")))
    (is (= "0" (logic/validate-date-parameter 0)))
    (is (= "123" (logic/validate-date-parameter 123))))

  (testing "returns nil when date is nil"
    (is (nil? (logic/validate-date-parameter nil))))

  (testing "returns nil when date is empty string"
    (is (nil? (logic/validate-date-parameter ""))))

  (testing "returns string as-is when date has whitespace (no trimming)"
    (is (= "   " (logic/validate-date-parameter "   ")))
    (is (= "  2024-01-15  " (logic/validate-date-parameter "  2024-01-15  "))))

  (testing "converts non-string values to string"
    (is (= "2024" (logic/validate-date-parameter 2024)))
    (is (= "true" (logic/validate-date-parameter true)))))

(deftest extract-query-filters-test
  (testing "extracts all query parameters when date is valid"
    (let [query-params {"date" "2024-01-15"
                        "activity" "Test Activity"
                        "activity_type" "Type A"
                        "type" "planned"}
          result (logic/extract-query-filters query-params)]
      (is (= "2024-01-15" (:date result)))
      (is (= "Test Activity" (:activity result)))
      (is (= "Type A" (:activity_type result)))
      (is (= "planned" (:type result)))))

  (testing "extracts only date when other parameters are missing"
    (let [query-params {"date" "2024-01-15"}
          result (logic/extract-query-filters query-params)]
      (is (= "2024-01-15" (:date result)))
      (is (nil? (:activity result)))
      (is (nil? (:activity_type result)))
      (is (nil? (:type result)))))

  (testing "returns nil when date is invalid"
    (let [query-params {"date" nil
                        "activity" "Test Activity"}
          result (logic/extract-query-filters query-params)]
      (is (nil? result))))

  (testing "returns nil when date is empty"
    (let [query-params {"date" ""
                        "activity" "Test Activity"}
          result (logic/extract-query-filters query-params)]
      (is (nil? result))))

  (testing "handles missing date key"
    (let [query-params {"activity" "Test Activity"}
          result (logic/extract-query-filters query-params)]
      (is (nil? result)))))

(deftest build-import-summary-test
  (testing "builds summary with valid rows and no errors"
    (let [parsed {:type :planned
                  :rows [{:date "2024-01-15" :activity "A"}]
                  :errors []}
          result (logic/build-import-summary parsed)]
      (is (= "planned" (:type result)))
      (is (= 1 (:valid result)))
      (is (= 0 (:invalid result)))
      (is (empty? (:errors result)))))

  (testing "builds summary with errors"
    (let [parsed {:type :executed
                  :rows [{:date "2024-01-15" :activity "A"}
                         {:date "2024-01-16" :activity "B"}]
                  :errors ["Error 1" "Error 2"]}
          result (logic/build-import-summary parsed)]
      (is (= "executed" (:type result)))
      (is (= 2 (:valid result)))
      (is (= 2 (:invalid result)))
      (is (= ["Error 1" "Error 2"] (:errors result)))))

  (testing "handles empty rows and errors"
    (let [parsed {:type :planned
                  :rows []
                  :errors []}
          result (logic/build-import-summary parsed)]
      (is (= "planned" (:type result)))
      (is (= 0 (:valid result)))
      (is (= 0 (:invalid result)))
      (is (empty? (:errors result)))))

  (testing "converts keyword type to string"
    (let [parsed {:type :executed
                  :rows [{:date "2024-01-15"}]
                  :errors []}
          result (logic/build-import-summary parsed)]
      (is (= "executed" (:type result))))))
