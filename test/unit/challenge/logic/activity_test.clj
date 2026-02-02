(ns challenge.logic.activity-test
  (:require [challenge.logic.activity :as logic.activity]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [schema.test :refer [validate-schemas]])
  (:import [java.time LocalDate]))

(use-fixtures :once validate-schemas)

(defn make-activity
  "Helper function to create a valid activity for testing"
  ([]
   (make-activity (LocalDate/now)))
  ([date]
   {:id nil
    :date date
    :activity "Test activity"
    :activity-type "work"
    :unit "hours"
    :amount-planned 8.0
    :amount-executed 6.0
    :created-at nil
    :updated-at nil}))

(deftest validate-activity-test
  (testing "validates activity successfully with valid data"
    (let [current-date (LocalDate/now)
          activity (make-activity current-date)]
      (is (= activity (logic.activity/validate-activity activity current-date)))))

  (testing "validates activity successfully when date is today"
    (let [current-date (LocalDate/now)
          activity (make-activity current-date)]
      (is (= activity (logic.activity/validate-activity activity current-date)))))

  (testing "validates activity successfully when date is in the past"
    (let [current-date (LocalDate/now)
          past-date (.minusDays current-date 1)
          activity (make-activity past-date)]
      (is (= activity (logic.activity/validate-activity activity current-date)))))

  (testing "throws exception when date is in the future"
    (let [current-date (LocalDate/now)
          future-date (.plusDays current-date 1)
          activity (make-activity future-date)]
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Activity date cannot be in the future"
                            (logic.activity/validate-activity activity current-date)))))

  (testing "throws exception when activity description is nil"
    ;; Note: Schema validation will catch nil before business logic
    ;; This test verifies schema validation works
    (let [current-date (LocalDate/now)
          activity (assoc (make-activity current-date) :activity nil)]
      (is (thrown? clojure.lang.ExceptionInfo
                   (logic.activity/validate-activity activity current-date)))))

  (testing "throws exception when activity description is empty string"
    (let [current-date (LocalDate/now)
          activity (assoc (make-activity current-date) :activity "")]
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Activity description cannot be empty"
                            (logic.activity/validate-activity activity current-date)))))

  (testing "throws exception when activity description is only whitespace"
    (let [current-date (LocalDate/now)
          activity (assoc (make-activity current-date) :activity "   ")]
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Activity description cannot be empty"
                            (logic.activity/validate-activity activity current-date)))))

  (testing "throws exception when amount-executed exceeds amount-planned"
    (let [current-date (LocalDate/now)
          activity (assoc (make-activity current-date)
                          :amount-planned 5.0
                          :amount-executed 10.0)]
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Amount executed cannot exceed amount planned"
                            (logic.activity/validate-activity activity current-date)))))

  (testing "accepts when amount-executed equals amount-planned"
    (let [current-date (LocalDate/now)
          activity (assoc (make-activity current-date)
                          :amount-planned 8.0
                          :amount-executed 8.0)]
      (is (= activity (logic.activity/validate-activity activity current-date)))))

  (testing "accepts when amount-executed is less than amount-planned"
    (let [current-date (LocalDate/now)
          activity (assoc (make-activity current-date)
                          :amount-planned 10.0
                          :amount-executed 5.0)]
      (is (= activity (logic.activity/validate-activity activity current-date)))))

  (testing "accepts when amount-planned is nil"
    (let [current-date (LocalDate/now)
          activity (assoc (make-activity current-date)
                          :amount-planned nil
                          :amount-executed 5.0)]
      (is (= activity (logic.activity/validate-activity activity current-date)))))

  (testing "accepts when amount-executed is nil"
    (let [current-date (LocalDate/now)
          activity (assoc (make-activity current-date)
                          :amount-planned 8.0
                          :amount-executed nil)]
      (is (= activity (logic.activity/validate-activity activity current-date)))))

  (testing "accepts when both amount-planned and amount-executed are nil"
    (let [current-date (LocalDate/now)
          activity (assoc (make-activity current-date)
                          :amount-planned nil
                          :amount-executed nil)]
      (is (= activity (logic.activity/validate-activity activity current-date))))))

(deftest can-update?-test
  (testing "returns false when existing-activity is nil"
    (let [current-date (LocalDate/now)
          updates {:date current-date}]
      (is (false? (logic.activity/can-update? nil updates current-date)))))

  (testing "returns false when updates contains date in the future"
    (let [current-date (LocalDate/now)
          future-date (.plusDays current-date 1)
          existing-activity (make-activity current-date)
          updates {:date future-date}]
      (is (false? (logic.activity/can-update? existing-activity updates current-date)))))

  (testing "returns true when existing-activity exists and date is not in the future"
    (let [current-date (LocalDate/now)
          existing-activity (make-activity current-date)
          updates {:date current-date}]
      (is (true? (logic.activity/can-update? existing-activity updates current-date)))))

  (testing "returns true when existing-activity exists and date is in the past"
    (let [current-date (LocalDate/now)
          past-date (.minusDays current-date 1)
          existing-activity (make-activity current-date)
          updates {:date past-date}]
      (is (true? (logic.activity/can-update? existing-activity updates current-date)))))

  (testing "returns true when updates does not contain date"
    (let [current-date (LocalDate/now)
          existing-activity (make-activity current-date)
          updates {:activity "Updated activity"}]
      (is (true? (logic.activity/can-update? existing-activity updates current-date)))))

  (testing "returns true when updates is empty"
    (let [current-date (LocalDate/now)
          existing-activity (make-activity current-date)
          updates {}]
      (is (true? (logic.activity/can-update? existing-activity updates current-date))))))
