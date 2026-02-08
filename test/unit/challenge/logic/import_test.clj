(ns challenge.logic.import-test
  (:require [challenge.logic.import :as logic.import]
            [clojure.test :refer [deftest is testing]])
  (:import [java.time LocalDate]))

(deftest parse-csv-rows-test
  (testing "parses CSV planned format (5th column = amount-planned)"
    (let [csv "Date,Activity,Activity type,Unit,Amount planned\n2025-01-16,Terrain leveling,Building,m3,4235.65"
          rows (logic.import/parse-csv-rows csv "planned")]
      (is (seq rows))
      (is (= 1 (count rows)))
      (is (= "2025-01-16" (:date (first rows))))
      (is (= "Terrain leveling" (:activity (first rows))))
      (is (= "Building" (:activity-type (first rows))))
      (is (= "m3" (:unit (first rows))))
      (is (= 4235.65 (:amount-planned (first rows))))
      (is (nil? (:amount-executed (first rows))))))

  (testing "parses CSV executed format (5th column = amount-executed)"
    (let [csv "Date,Activity,Activity type,Unit,Amount executed\n2025-07-21,Cement making,Building,m3,3901.2"
          rows (logic.import/parse-csv-rows csv "executed")]
      (is (seq rows))
      (is (= 1 (count rows)))
      (is (= "2025-07-21" (:date (first rows))))
      (is (= "Cement making" (:activity (first rows))))
      (is (nil? (:amount-planned (first rows))))
      (is (= 3901.2 (:amount-executed (first rows))))))

  (testing "parses multiple rows"
    (let [csv (str "Date,Activity,Activity type,Unit,Amount planned\n"
                   "2025-01-16,A1,Building,m3,100\n"
                   "2025-01-17,A2,Road,m2,200")
          rows (logic.import/parse-csv-rows csv "planned")]
      (is (= 2 (count rows)))
      (is (= "2025-01-16" (:date (first rows))))
      (is (= "2025-01-17" (:date (second rows))))))

  (testing "returns nil for nil csv, empty seq for empty string"
    (is (nil? (logic.import/parse-csv-rows nil "planned")))
    (is (empty? (logic.import/parse-csv-rows "" "planned")))))

(deftest process-import-rows-test
  (testing "counts valid and invalid rows"
    (let [current-date (LocalDate/of 2025 6 1)
          valid-request {:date "2025-01-16" :activity "A" :activity-type "B" :unit "m" :amount-planned 1.0 :amount-executed nil}
          future-request {:date "2030-01-01" :activity "A" :activity-type "B" :unit "m" :amount-planned 1.0 :amount-executed nil}
          requests [valid-request future-request]
          result (logic.import/process-import-rows requests current-date)]
      (is (= 1 (count (:valid result))))
      (is (= 1 (:invalid result))))))
