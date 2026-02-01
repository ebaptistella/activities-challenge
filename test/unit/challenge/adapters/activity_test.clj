(ns challenge.adapters.activity-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [schema.test :refer [validate-schemas]]
            [challenge.adapters.activity :as adapters.activity])
  (:import [java.time LocalDate Instant]
           [java.sql Date Timestamp]))

(use-fixtures :once validate-schemas)

(deftest wire->model-test
  (testing "converts wire.in to model correctly"
    (let [wire-data {:date "2024-01-15"
                     :activity "Test activity"
                     :activity-type "work"
                     :unit "hours"
                     :amount-planned 8.0
                     :amount-executed 6.0}
          result (adapters.activity/wire->model wire-data)]
      (is (= (LocalDate/of 2024 1 15) (:date result)))
      (is (= "Test activity" (:activity result)))
      (is (= "work" (:activity-type result)))
      (is (= "hours" (:unit result)))
      (is (= 8.0 (:amount-planned result)))
      (is (= 6.0 (:amount-executed result)))
      (is (nil? (:id result)))
      (is (nil? (:created-at result)))
      (is (nil? (:updated-at result)))))

  (testing "converts string date to LocalDate"
    (let [wire-data {:date "2024-12-25"
                     :activity "Test"
                     :activity-type "work"
                     :unit "hours"}
          result (adapters.activity/wire->model wire-data)]
      (is (= (LocalDate/of 2024 12 25) (:date result)))))

  (testing "handles optional fields as nil"
    (let [wire-data {:date "2024-01-15"
                     :activity "Test activity"
                     :activity-type "work"
                     :unit "hours"}
          result (adapters.activity/wire->model wire-data)]
      (is (nil? (:amount-planned result)))
      (is (nil? (:amount-executed result))))))

(deftest update-wire->model-test
  (testing "converts update wire to model"
    (let [wire-data {:date "2024-01-15"
                     :activity "Updated activity"
                     :activity-type "work"
                     :unit "hours"
                     :amount-planned 10.0}
          result (adapters.activity/update-wire->model wire-data)]
      (is (= (LocalDate/of 2024 1 15) (:date result)))
      (is (= "Updated activity" (:activity result)))
      (is (= 10.0 (:amount-planned result)))))

  (testing "handles optional fields"
    (let [wire-data {:date "2024-01-15"
                     :activity "Updated activity"
                     :activity-type "work"
                     :unit "hours"}
          result (adapters.activity/update-wire->model wire-data)]
      (is (nil? (:amount-planned result)))
      (is (nil? (:amount-executed result))))))

(deftest model->wire-test
  (testing "converts model to wire.out correctly"
    (let [model {:id 1
                 :date (LocalDate/of 2024 1 15)
                 :activity "Test activity"
                 :activity-type "work"
                 :unit "hours"
                 :amount-planned 8.0
                 :amount-executed 6.0
                 :created-at (Instant/parse "2024-01-15T10:00:00Z")
                 :updated-at (Instant/parse "2024-01-15T11:00:00Z")}
          result (adapters.activity/model->wire model)]
      (is (= 1 (:id result)))
      (is (= "2024-01-15" (:date result)))
      (is (= "Test activity" (:activity result)))
      (is (= "work" (:activity-type result)))
      (is (= "hours" (:unit result)))
      (is (= 8.0 (:amount-planned result)))
      (is (= 6.0 (:amount-executed result)))
      (is (= "2024-01-15T10:00:00Z" (:created-at result)))
      (is (= "2024-01-15T11:00:00Z" (:updated-at result)))))

  (testing "converts LocalDate to string"
    (let [model {:id 1
                 :date (LocalDate/of 2024 12 25)
                 :activity "Test"
                 :activity-type "work"
                 :unit "hours"}
          result (adapters.activity/model->wire model)]
      (is (= "2024-12-25" (:date result)))))

  (testing "converts Instant to string"
    (let [model {:id 1
                 :date (LocalDate/of 2024 1 15)
                 :activity "Test"
                 :activity-type "work"
                 :unit "hours"
                 :created-at (Instant/parse "2024-01-15T10:00:00Z")}
          result (adapters.activity/model->wire model)]
      (is (= "2024-01-15T10:00:00Z" (:created-at result)))))

  (testing "handles nil fields"
    (let [model {:id 1
                 :date (LocalDate/of 2024 1 15)
                 :activity "Test"
                 :activity-type "work"
                 :unit "hours"
                 :amount-planned nil
                 :amount-executed nil
                 :created-at nil
                 :updated-at nil}
          result (adapters.activity/model->wire model)]
      (is (nil? (:amount-planned result)))
      (is (nil? (:amount-executed result)))
      (is (nil? (:created-at result)))
      (is (nil? (:updated-at result))))))

(deftest model->persistency-test
  (testing "converts model to persistency format correctly"
    (let [model {:id 1
                 :date (LocalDate/of 2024 1 15)
                 :activity "Test activity"
                 :activity-type "work"
                 :unit "hours"
                 :amount-planned 8.0
                 :amount-executed 6.0
                 :created-at (Instant/parse "2024-01-15T10:00:00Z")
                 :updated-at (Instant/parse "2024-01-15T11:00:00Z")}
          result (adapters.activity/model->persistency model)]
      (is (= 1 (:activity/id result)))
      (is (= (LocalDate/of 2024 1 15) (:activity/date result)))
      (is (= "Test activity" (:activity/activity result)))
      (is (= "work" (:activity/activity-type result)))
      (is (= "hours" (:activity/unit result)))
      (is (= 8.0 (:activity/amount-planned result)))
      (is (= 6.0 (:activity/amount-executed result)))
      (is (= (Instant/parse "2024-01-15T10:00:00Z") (:activity/created-at result)))
      (is (= (Instant/parse "2024-01-15T11:00:00Z") (:activity/updated-at result)))))

  (testing "removes nil fields"
    (let [model {:id 1
                 :date (LocalDate/of 2024 1 15)
                 :activity "Test activity"
                 :activity-type "work"
                 :unit "hours"
                 :amount-planned nil
                 :amount-executed nil
                 :created-at nil
                 :updated-at nil}
          result (adapters.activity/model->persistency model)]
      (is (not (contains? result :activity/amount-planned)))
      (is (not (contains? result :activity/amount-executed)))
      (is (not (contains? result :activity/created-at)))
      (is (not (contains? result :activity/updated-at)))))

  (testing "uses namespaced keys"
    (let [model {:id 1
                 :date (LocalDate/of 2024 1 15)
                 :activity "Test"
                 :activity-type "work"
                 :unit "hours"}
          result (adapters.activity/model->persistency model)]
      (is (= "work" (:activity/activity-type result)))
      (is (not (contains? result :activity-type))))))

(deftest persistency->model-test
  (testing "converts persistency to model correctly with namespaced keys"
    (let [persistency-data {:activity/id 1
                            :activity/date (LocalDate/of 2024 1 15)
                            :activity/activity "Test activity"
                            :activity/activity-type "work"
                            :activity/unit "hours"
                            :activity/amount-planned 8.0
                            :activity/amount-executed 6.0
                            :activity/created-at (Instant/parse "2024-01-15T10:00:00Z")
                            :activity/updated-at (Instant/parse "2024-01-15T11:00:00Z")}
          result (adapters.activity/persistency->model persistency-data)]
      (is (= 1 (:id result)))
      (is (= (LocalDate/of 2024 1 15) (:date result)))
      (is (= "Test activity" (:activity result)))
      (is (= "work" (:activity-type result)))
      (is (= "hours" (:unit result)))
      (is (= 8.0 (:amount-planned result)))
      (is (= 6.0 (:amount-executed result)))
      (is (= (Instant/parse "2024-01-15T10:00:00Z") (:created-at result)))
      (is (= (Instant/parse "2024-01-15T11:00:00Z") (:updated-at result)))))

  (testing "converts persistency to model correctly with non-namespaced keys (from database)"
    (let [persistency-data {:id 1
                            :date (LocalDate/of 2024 1 15)
                            :activity "Test activity"
                            :activity_type "work"
                            :unit "hours"
                            :amount_planned 8.0
                            :amount_executed 6.0
                            :created_at (Instant/parse "2024-01-15T10:00:00Z")
                            :updated_at (Instant/parse "2024-01-15T11:00:00Z")}
          result (adapters.activity/persistency->model persistency-data)]
      (is (= 1 (:id result)))
      (is (= (LocalDate/of 2024 1 15) (:date result)))
      (is (= "Test activity" (:activity result)))
      (is (= "work" (:activity-type result)))
      (is (= "hours" (:unit result)))
      (is (= 8.0 (:amount-planned result)))
      (is (= 6.0 (:amount-executed result)))
      (is (= (Instant/parse "2024-01-15T10:00:00Z") (:created-at result)))
      (is (= (Instant/parse "2024-01-15T11:00:00Z") (:updated-at result)))))

  (testing "converts java.sql.Date to LocalDate"
    (let [sql-date (Date/valueOf (LocalDate/of 2024 1 15))
          persistency-data {:id 1
                            :date sql-date
                            :activity "Test"
                            :activity_type "work"
                            :unit "hours"}
          result (adapters.activity/persistency->model persistency-data)]
      (is (= (LocalDate/of 2024 1 15) (:date result)))))

  (testing "converts string date to LocalDate"
    (let [persistency-data {:id 1
                            :date "2024-01-15"
                            :activity "Test"
                            :activity_type "work"
                            :unit "hours"}
          result (adapters.activity/persistency->model persistency-data)]
      (is (= (LocalDate/of 2024 1 15) (:date result)))))

  (testing "handles LocalDate as-is"
    (let [local-date (LocalDate/of 2024 1 15)
          persistency-data {:id 1
                            :date local-date
                            :activity "Test"
                            :activity_type "work"
                            :unit "hours"}
          result (adapters.activity/persistency->model persistency-data)]
      (is (= local-date (:date result)))))

  (testing "converts java.sql.Timestamp to Instant"
    (let [timestamp (Timestamp/from (Instant/parse "2024-01-15T10:00:00Z"))
          persistency-data {:id 1
                            :date (LocalDate/of 2024 1 15)
                            :activity "Test"
                            :activity_type "work"
                            :unit "hours"
                            :created_at timestamp}
          result (adapters.activity/persistency->model persistency-data)]
      (is (= (Instant/parse "2024-01-15T10:00:00Z") (:created-at result)))))

  (testing "converts string timestamp to Instant"
    (let [persistency-data {:id 1
                            :date (LocalDate/of 2024 1 15)
                            :activity "Test"
                            :activity_type "work"
                            :unit "hours"
                            :created_at "2024-01-15T10:00:00Z"}
          result (adapters.activity/persistency->model persistency-data)]
      (is (= (Instant/parse "2024-01-15T10:00:00Z") (:created-at result)))))

  (testing "handles Instant as-is"
    (let [instant (Instant/parse "2024-01-15T10:00:00Z")
          persistency-data {:id 1
                            :date (LocalDate/of 2024 1 15)
                            :activity "Test"
                            :activity_type "work"
                            :unit "hours"
                            :created_at instant}
          result (adapters.activity/persistency->model persistency-data)]
      (is (= instant (:created-at result)))))

  (testing "converts snake_case to kebab-case"
    (let [persistency-data {:id 1
                            :date (LocalDate/of 2024 1 15)
                            :activity "Test"
                            :activity_type "work"
                            :unit "hours"}
          result (adapters.activity/persistency->model persistency-data)]
      (is (= "work" (:activity-type result)))
      (is (not (contains? result :activity_type)))))

  (testing "handles nil fields"
    (let [persistency-data {:id 1
                            :date (LocalDate/of 2024 1 15)
                            :activity "Test"
                            :activity_type "work"
                            :unit "hours"
                            :amount_planned nil
                            :amount_executed nil
                            :created_at nil
                            :updated_at nil}
          result (adapters.activity/persistency->model persistency-data)]
      (is (nil? (:amount-planned result)))
      (is (nil? (:amount-executed result)))
      (is (nil? (:created-at result)))
      (is (nil? (:updated-at result))))))
