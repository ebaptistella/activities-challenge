(ns challenge.integration.query-activities-test
  "Integration tests for activities query functionality."
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [clojure.data.json :as json]
            [challenge.integration.aux.init :as aux]
            [challenge.api.infrastructure.database :as database]))

(def test-system (atom nil))
(def database-available? (atom false))

(defn setup-system [f]
  (try
    (let [system (aux/create-test-system)
          started-system (aux/start-test-system system)
          ds (:datasource (:database started-system))]
      (if (aux/check-database-available? ds)
        (do
          (reset! database-available? true)
          (reset! test-system started-system)
          (f)
          (aux/stop-test-system started-system)
          (reset! test-system nil))
        (do
          (reset! database-available? false)
          (println "Skipping integration tests: database not available"))))
    (catch Exception e
      (reset! database-available? false)
      (println "Skipping integration tests: failed to start system" (.getMessage e)))))

(defn clear-before-test [f]
  (when @database-available?
    (let [ds (:datasource (:database @test-system))]
      (aux/clear-database ds)))
  (f))

(use-fixtures :once setup-system)
(use-fixtures :each clear-before-test)

(defn seed-test-data
  "Seeds test database with sample activities."
  [ds]
  (database/import-planned-batch! ds
                                  [{:date "2024-01-15" :activity "Activity A" :activity_type "Type 1" :unit "kg" :amount 100.0M}
                                   {:date "2024-01-15" :activity "Activity B" :activity_type "Type 2" :unit "m" :amount 200.0M}
                                   {:date "2024-01-15" :activity "Activity C" :activity_type "Type 1" :unit "kg" :amount 150.0M}
                                   {:date "2024-01-16" :activity "Activity A" :activity_type "Type 1" :unit "kg" :amount 120.0M}])

  (database/import-executed-batch! ds
                                   [{:date "2024-01-15" :activity "Activity A" :activity_type "Type 1" :unit "kg" :amount 80.0M}
                                    {:date "2024-01-15" :activity "Activity B" :activity_type "Type 2" :unit "m" :amount 180.0M}
                                    {:date "2024-01-15" :activity "Activity C" :activity_type "Type 1" :unit "kg" :amount 160.0M}]))

(deftest query-activities-by-date-test
  (when @database-available?
    (testing "queries activities filtered by date"
      (let [ds (:datasource (:database @test-system))
            handler (:handler (:router @test-system))]
        (seed-test-data ds)

        (let [request (aux/create-query-request "/api/activities" {"date" "2024-01-15"})
              response (aux/handler-request handler request)
              body (json/read-str (:body response) :key-fn keyword)]
          (is (= 200 (:status response)))
          (is (= 3 (count (:items body))))
          (is (every? #(= "2024-01-15" (str (:date %))) (:items body))))))))

(deftest query-activities-with-activity-filter-test
  (when @database-available?
    (testing "queries activities filtered by activity name"
      (let [ds (:datasource (:database @test-system))
            handler (:handler (:router @test-system))]
        (seed-test-data ds)

        (let [request (aux/create-query-request "/api/activities" {"date" "2024-01-15" "activity" "Activity A"})
              response (aux/handler-request handler request)
              body (json/read-str (:body response) :key-fn keyword)]
          (is (= 200 (:status response)))
          (is (= 1 (count (:items body))))
          (is (= "Activity A" (:activity (first (:items body))))))))))

(deftest query-activities-with-activity-type-filter-test
  (when @database-available?
    (testing "queries activities filtered by activity type"
      (let [ds (:datasource (:database @test-system))
            handler (:handler (:router @test-system))]
        (seed-test-data ds)

        (let [request (aux/create-query-request "/api/activities" {"date" "2024-01-15" "activity_type" "Type 1"})
              response (aux/handler-request handler request)
              body (json/read-str (:body response) :key-fn keyword)]
          (is (= 200 (:status response)))
          (is (= 2 (count (:items body))))
          (is (every? #(= "Type 1" (:activity_type %)) (:items body))))))))

(deftest query-activities-with-type-filter-planned-test
  (when @database-available?
    (testing "queries activities filtered by type 'planned'"
      (let [ds (:datasource (:database @test-system))
            handler (:handler (:router @test-system))]
        (seed-test-data ds)

        (let [request (aux/create-query-request "/api/activities" {"date" "2024-01-15" "type" "planned"})
              response (aux/handler-request handler request)
              body (json/read-str (:body response) :key-fn keyword)]
          (is (= 200 (:status response)))
          (is (= 3 (count (:items body))))
          (is (every? #(= "planned" (:kind %)) (:items body)))
          (is (every? #(some? (:amount %)) (:items body))))))))

(deftest query-activities-with-type-filter-executed-test
  (when @database-available?
    (testing "queries activities filtered by type 'executed'"
      (let [ds (:datasource (:database @test-system))
            handler (:handler (:router @test-system))]
        (seed-test-data ds)

        (let [request (aux/create-query-request "/api/activities" {"date" "2024-01-15" "type" "executed"})
              response (aux/handler-request handler request)
              body (json/read-str (:body response) :key-fn keyword)]
          (is (= 200 (:status response)))
          (is (= 3 (count (:items body))))
          (is (every? #(= "executed" (:kind %)) (:items body))))))))

(deftest query-activities-with-type-filter-both-test
  (when @database-available?
    (testing "queries activities with both planned and executed amounts"
      (let [ds (:datasource (:database @test-system))
            handler (:handler (:router @test-system))]
        (seed-test-data ds)

        (let [request (aux/create-query-request "/api/activities" {"date" "2024-01-15"})
              response (aux/handler-request handler request)
              body (json/read-str (:body response) :key-fn keyword)]
          (is (= 200 (:status response)))
          (is (= 3 (count (:items body))))
          (is (every? #(= "executed" (:kind %)) (:items body)))
          (is (every? #(some? (:amount %)) (:items body))))))))

(deftest query-activities-missing-date-test
  (when @database-available?
    (testing "returns error when date parameter is missing"
      (let [handler (:handler (:router @test-system))
            request (aux/create-query-request "/api/activities" {})
            response (aux/handler-request handler request)
            body (json/read-str (:body response) :key-fn keyword)]
        (is (= 400 (:status response)))
        (is (some? (:error body)))
        (is (= "Parameter 'date' is required" (:error body)))))))

(deftest query-activities-empty-date-test
  (when @database-available?
    (testing "returns error when date parameter is empty"
      (let [handler (:handler (:router @test-system))
            request (aux/create-query-request "/api/activities" {"date" ""})
            response (aux/handler-request handler request)
            body (json/read-str (:body response) :key-fn keyword)]
        (is (= 400 (:status response)))
        (is (some? (:error body)))))))

(deftest query-activities-no-results-test
  (when @database-available?
    (testing "returns empty results for date with no activities"
      (let [ds (:datasource (:database @test-system))
            handler (:handler (:router @test-system))]
        (seed-test-data ds)

        (let [request (aux/create-query-request "/api/activities" {"date" "2024-12-31"})
              response (aux/handler-request handler request)
              body (json/read-str (:body response) :key-fn keyword)]
          (is (= 200 (:status response)))
          (is (= 0 (count (:items body)))))))))

(deftest query-activities-combined-filters-test
  (when @database-available?
    (testing "queries activities with multiple filters combined"
      (let [ds (:datasource (:database @test-system))
            handler (:handler (:router @test-system))]
        (seed-test-data ds)

        (let [request (aux/create-query-request "/api/activities"
                                                {"date" "2024-01-15"
                                                 "activity" "Activity A"
                                                 "activity_type" "Type 1"
                                                 "type" "executed"})
              response (aux/handler-request handler request)
              body (json/read-str (:body response) :key-fn keyword)]
          (is (= 200 (:status response)))
          (is (= 1 (count (:items body))))
          (let [activity (first (:items body))]
            (is (= "Activity A" (:activity activity)))
            (is (= "Type 1" (:activity_type activity)))
            (is (= "executed" (:kind activity)))))))))
