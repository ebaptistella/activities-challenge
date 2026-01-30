(ns challenge.integration.end-to-end-test
  "End-to-end integration tests for complete business flows."
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [clojure.data.json :as json]
            [challenge.integration.aux.init :as aux]))

(defn- normalize-amount
  "Normalizes amount value to BigDecimal for comparison.
  
  Parameters:
  - amount: Number (BigDecimal, Double, etc.) or nil
  
  Returns:
  - BigDecimal or nil"
  [amount]
  (when amount
    (bigdec amount)))

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

(deftest complete-workflow-test
  (when @database-available?
    (testing "complete workflow: import planned, import executed, query activities"
    (let [handler (:handler (:router @test-system))
          
          ;; Step 1: Import planned activities
          planned-csv "Date,Activity,Activity type,Unit,Amount planned
2024-01-15,Development,Engineering,hours,8.0
2024-01-15,Testing,QA,hours,4.0
2024-01-15,Review,Engineering,hours,2.0
2024-01-16,Development,Engineering,hours,6.0"
          planned-file (aux/create-csv-file planned-csv)
          planned-request (aux/create-multipart-request planned-file "/api/import")
          planned-response (aux/handler-request handler planned-request)
          planned-body (json/read-str (:body planned-response) :key-fn keyword)]
      
      (is (= 200 (:status planned-response)))
      (is (= "planned" (:type planned-body)))
      (is (= 4 (:valid planned-body)))
      (is (= 0 (:invalid planned-body)))
      
      ;; Step 2: Import executed activities
      (let [executed-csv "Date,Activity,Activity type,Unit,Amount executed
2024-01-15,Development,Engineering,hours,7.5
2024-01-15,Testing,QA,hours,4.5
2024-01-15,Review,Engineering,hours,2.0"
            executed-file (aux/create-csv-file executed-csv)
            executed-request (aux/create-multipart-request executed-file "/api/import")
            executed-response (aux/handler-request handler executed-request)
            executed-body (json/read-str (:body executed-response) :key-fn keyword)]
        
        (is (= 200 (:status executed-response)))
        (is (= "executed" (:type executed-body)))
        (is (= 3 (:valid executed-body)))
        
        ;; Step 3: Query activities for 2024-01-15
        (let [query-request (aux/create-query-request "/api/activities" {"date" "2024-01-15"})
              query-response (aux/handler-request handler query-request)
              query-body (json/read-str (:body query-response) :key-fn keyword)]
          
          (is (= 200 (:status query-response)))
          (is (= 3 (count (:items query-body))))
          
          ;; Verify activities have correct data
          (let [activities (:items query-body)
                development (first (filter #(= "Development" (:activity %)) activities))
                testing (first (filter #(= "Testing" (:activity %)) activities))
                review (first (filter #(= "Review" (:activity %)) activities))]
            
            (is (some? development))
            (is (= "executed" (:kind development)))
            (is (= 7.5M (normalize-amount (:amount development))))
            (is (= "Engineering" (:activity_type development)))
            (is (= "hours" (:unit development)))
            
            (is (some? testing))
            (is (= "executed" (:kind testing)))
            (is (= 4.5M (normalize-amount (:amount testing))))
            (is (= "QA" (:activity_type testing)))
            
            (is (some? review))
            (is (= "executed" (:kind review)))
            (is (= 2.0M (normalize-amount (:amount review))))
            
            ;; Step 4: Query activities for 2024-01-16 (only planned)
            (let [query-request-16 (aux/create-query-request "/api/activities" {"date" "2024-01-16"})
                  query-response-16 (aux/handler-request handler query-request-16)
                  query-body-16 (json/read-str (:body query-response-16) :key-fn keyword)]
              
              (is (= 200 (:status query-response-16)))
              (is (= 1 (count (:items query-body-16))))
              
              (let [activity-16 (first (:items query-body-16))]
                (is (= "Development" (:activity activity-16)))
                (is (= "planned" (:kind activity-16)))
                (is (= 6.0M (normalize-amount (:amount activity-16)))))))))))))

(deftest filter-activities-by-type-test
  (when @database-available?
    (testing "complete workflow with type filtering"
    (let [handler (:handler (:router @test-system))
          
          ;; Import both planned and executed
          planned-csv "Date,Activity,Activity type,Unit,Amount planned
2024-01-15,Task A,Type 1,kg,100.0
2024-01-15,Task B,Type 2,m,200.0"
          planned-file (aux/create-csv-file planned-csv)
          planned-request (aux/create-multipart-request planned-file "/api/import")
          _ (aux/handler-request handler planned-request)
          
          executed-csv "Date,Activity,Activity type,Unit,Amount executed
2024-01-15,Task A,Type 1,kg,90.0
2024-01-15,Task B,Type 2,m,180.0"
          executed-file (aux/create-csv-file executed-csv)
          executed-request (aux/create-multipart-request executed-file "/api/import")
          _ (aux/handler-request handler executed-request)]
      
      ;; Query with type filter "planned"
      (let [planned-query (aux/create-query-request "/api/activities" {"date" "2024-01-15" "type" "planned"})
            planned-response (aux/handler-request handler planned-query)
            planned-body (json/read-str (:body planned-response) :key-fn keyword)]
        (is (= 200 (:status planned-response)))
        (is (= 2 (count (:items planned-body))))
        (is (every? #(= "planned" (:kind %)) (:items planned-body)))
        (is (every? #(= 100.0M (normalize-amount (:amount %))) (filter #(= "Task A" (:activity %)) (:items planned-body)))))
      
      ;; Query with type filter "executed"
      (let [executed-query (aux/create-query-request "/api/activities" {"date" "2024-01-15" "type" "executed"})
            executed-response (aux/handler-request handler executed-query)
            executed-body (json/read-str (:body executed-response) :key-fn keyword)]
        (is (= 200 (:status executed-response)))
        (is (= 2 (count (:items executed-body))))
        (is (every? #(= "executed" (:kind %)) (:items executed-body)))
        (is (every? #(= 90.0M (normalize-amount (:amount %))) (filter #(= "Task A" (:activity %)) (:items executed-body)))))))))

(deftest filter-activities-by-activity-name-test
  (when @database-available?
    (testing "complete workflow with activity name filtering"
    (let [handler (:handler (:router @test-system))
          
          ;; Import activities
          planned-csv "Date,Activity,Activity type,Unit,Amount planned
2024-01-15,Alpha,Type A,kg,100.0
2024-01-15,Beta,Type B,m,200.0
2024-01-15,Alpha,Type C,l,150.0"
          planned-file (aux/create-csv-file planned-csv)
          planned-request (aux/create-multipart-request planned-file "/api/import")
          _ (aux/handler-request handler planned-request)]
      
      ;; Query filtered by activity name
      (let [query (aux/create-query-request "/api/activities" {"date" "2024-01-15" "activity" "Alpha"})
            response (aux/handler-request handler query)
            body (json/read-str (:body response) :key-fn keyword)]
        (is (= 200 (:status response)))
        (is (= 2 (count (:items body))))
        (is (every? #(= "Alpha" (:activity %)) (:items body))))))))

(deftest filter-activities-by-activity-type-test
  (when @database-available?
    (testing "complete workflow with activity type filtering"
    (let [handler (:handler (:router @test-system))
          
          ;; Import activities
          planned-csv "Date,Activity,Activity type,Unit,Amount planned
2024-01-15,Task 1,Engineering,hours,8.0
2024-01-15,Task 2,QA,hours,4.0
2024-01-15,Task 3,Engineering,hours,6.0"
          planned-file (aux/create-csv-file planned-csv)
          planned-request (aux/create-multipart-request planned-file "/api/import")
          _ (aux/handler-request handler planned-request)]
      
      ;; Query filtered by activity type
      (let [query (aux/create-query-request "/api/activities" {"date" "2024-01-15" "activity_type" "Engineering"})
            response (aux/handler-request handler query)
            body (json/read-str (:body response) :key-fn keyword)]
        (is (= 200 (:status response)))
        (is (= 2 (count (:items body))))
        (is (every? #(= "Engineering" (:activity_type %)) (:items body))))))))

(deftest update-existing-activities-test
  (when @database-available?
    (testing "complete workflow: import, update, query"
    (let [handler (:handler (:router @test-system))
          
          ;; Initial import
          first-csv "Date,Activity,Activity type,Unit,Amount planned
2024-01-15,Initial Task,Type A,kg,100.0"
          first-file (aux/create-csv-file first-csv)
          first-request (aux/create-multipart-request first-file "/api/import")
          first-response (aux/handler-request handler first-request)]
      
      (is (= 200 (:status first-response)))
      
      ;; Update with new amount
      (let [update-csv "Date,Activity,Activity type,Unit,Amount planned
2024-01-15,Initial Task,Type A,kg,150.0"
            update-file (aux/create-csv-file update-csv)
            update-request (aux/create-multipart-request update-file "/api/import")
            update-response (aux/handler-request handler update-request)]
        
        (is (= 200 (:status update-response)))
        
        ;; Query to verify update
        (let [query (aux/create-query-request "/api/activities" {"date" "2024-01-15"})
              response (aux/handler-request handler query)
              body (json/read-str (:body response) :key-fn keyword)]
          (is (= 200 (:status response)))
          (is (= 1 (count (:items body))))
          (is (= 150.0M (normalize-amount (:amount (first (:items body))))))))))))
