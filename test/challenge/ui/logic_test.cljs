(ns challenge.ui.logic-test
  "Unit tests for frontend pure business logic functions."
  (:require [cljs.test :refer-macros [deftest testing is]]
            [challenge.ui.logic :as logic]))

(deftest format-date-test
  (testing "formats valid ISO date string to YYYY-MM-DD"
    (is (= "2024-01-15" (logic/format-date "2024-01-15T00:00:00Z")))
    (is (= "2024-12-31" (logic/format-date "2024-12-31T23:59:59Z")))
    (is (= "2024-03-05" (logic/format-date "2024-03-05T12:30:00Z"))))

  (testing "returns empty string when date-str is empty"
    (is (= "" (logic/format-date ""))))

  (testing "handles different date formats"
    (let [date-str "2024-01-15T10:30:00.000Z"]
      (is (= "2024-01-15" (logic/format-date date-str))))))

(deftest today-date-test
  (testing "returns date in YYYY-MM-DD format"
    (let [result (logic/today-date)]
      (is (string? result))
      (is (= 10 (count result)))
      (is (= 4 (count (re-seq #"\d" (subs result 0 4)))))
      (is (= "-" (subs result 4 5)))
      (is (= "-" (subs result 7 8))))))

(deftest build-query-params-test
  (testing "builds query string with all filters"
    (let [filters {:date "2024-01-15"
                    :activity "Test Activity"
                    :activity-type "Type A"}
          result (logic/build-query-params filters)]
      (is (string? result))
      (is (not (empty? result)))
      (is (some? (re-find #"date=" result)))
      (is (some? (re-find #"activity=" result)))
      (is (some? (re-find #"activity_type=" result)))))

  (testing "uses today's date when date is empty"
    (let [filters {:date ""
                    :activity "Test Activity"}
          result (logic/build-query-params filters)]
      (is (string? result))
      (is (some? (re-find #"date=" result)))))

  (testing "uses today's date when date is nil"
    (let [filters {:date nil
                    :activity "Test Activity"}
          result (logic/build-query-params filters)]
      (is (string? result))
      (is (some? (re-find #"date=" result)))))

  (testing "excludes empty activity filter"
    (let [filters {:date "2024-01-15"
                    :activity ""
                    :activity-type ""}
          result (logic/build-query-params filters)]
      (is (not (some? (re-find #"activity=" result))))
      (is (not (some? (re-find #"activity_type=" result))))))

  (testing "includes only non-empty filters"
    (let [filters {:date "2024-01-15"
                    :activity "Test Activity"
                    :activity-type ""}
          result (logic/build-query-params filters)]
      (is (some? (re-find #"date=" result)))
      (is (some? (re-find #"activity=" result)))
      (is (not (some? (re-find #"activity_type=" result))))))

  (testing "URL encodes filter values"
    (let [filters {:date "2024-01-15"
                    :activity "Test Activity & More"}
          result (logic/build-query-params filters)]
      (is (string? result))
      (is (some? (re-find #"date=2024-01-15" result))))))

(deftest activity-type-label-test
  (testing "returns formatted label for keyword types"
    (is (= "Planned" (logic/activity-type-label :planned)))
    (is (= "Executed" (logic/activity-type-label :executed)))
    (is (= "Both" (logic/activity-type-label :both))))

  (testing "returns formatted label for string types"
    (is (= "Planned" (logic/activity-type-label "planned")))
    (is (= "Executed" (logic/activity-type-label "executed")))
    (is (= "Both" (logic/activity-type-label "both"))))

  (testing "returns type as string for unknown types"
    (is (= "unknown" (logic/activity-type-label "unknown")))
    (is (= "custom" (logic/activity-type-label :custom)))
    (is (= "test" (logic/activity-type-label "test"))))

  (testing "handles nil gracefully"
    (is (= "nil" (logic/activity-type-label nil)))))

(deftest activity-type-class-test
  (testing "returns CSS classes for keyword types"
    (is (= "bg-blue-100 text-blue-800" (logic/activity-type-class :planned)))
    (is (= "bg-green-100 text-green-800" (logic/activity-type-class :executed)))
    (is (= "bg-purple-100 text-purple-800" (logic/activity-type-class :both))))

  (testing "returns CSS classes for string types"
    (is (= "bg-blue-100 text-blue-800" (logic/activity-type-class "planned")))
    (is (= "bg-green-100 text-green-800" (logic/activity-type-class "executed")))
    (is (= "bg-purple-100 text-purple-800" (logic/activity-type-class "both"))))

  (testing "returns default classes for unknown types"
    (is (= "bg-gray-100 text-gray-800" (logic/activity-type-class "unknown")))
    (is (= "bg-gray-100 text-gray-800" (logic/activity-type-class :custom)))
    (is (= "bg-gray-100 text-gray-800" (logic/activity-type-class "test"))))

  (testing "handles nil gracefully"
    (is (= "bg-gray-100 text-gray-800" (logic/activity-type-class nil)))))

(deftest default-filters-test
  (testing "returns default filters with today's date"
    (let [result (logic/default-filters)]
      (is (map? result))
      (is (string? (:date result)))
      (is (= 10 (count (:date result))))
      (is (= "" (:activity result)))
      (is (= "" (:activity-type result)))))

  (testing "date is in YYYY-MM-DD format"
    (let [result (logic/default-filters)
          date (:date result)]
      (is (re-matches #"\d{4}-\d{2}-\d{2}" date)))))
