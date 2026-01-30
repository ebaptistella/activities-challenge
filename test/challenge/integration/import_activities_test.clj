(ns challenge.integration.import-activities-test
  "Integration tests for CSV import functionality."
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

(deftest import-planned-activities-test
  (when @database-available?
    (testing "imports planned activities CSV successfully"
      (let [csv-content "Date,Activity,Activity type,Unit,Amount planned
2024-01-15,Test Activity,Type A,kg,100.5
2024-01-15,Another Activity,Type B,m,200.0"
            csv-file (aux/create-csv-file csv-content)
            handler (:handler (:router @test-system))
            request (aux/create-multipart-request csv-file "/api/import")
            response (aux/handler-request handler request)
            body (json/read-str (:body response) :key-fn keyword)]
        (is (= 200 (:status response)))
        (is (= "planned" (:type body)))
        (is (= 2 (:valid body)))
        (is (= 0 (:invalid body)))
        (is (empty? (:errors body)))))))

(deftest import-executed-activities-test
  (when @database-available?
    (testing "imports executed activities CSV successfully"
    (let [csv-content "Date,Activity,Activity type,Unit,Amount executed
2024-01-15,Test Activity,Type A,kg,80.0
2024-01-15,Another Activity,Type B,m,150.0"
          csv-file (aux/create-csv-file csv-content)
          handler (:handler (:router @test-system))
          request (aux/create-multipart-request csv-file "/api/import")
          response (aux/handler-request handler request)
          body (json/read-str (:body response) :key-fn keyword)]
      (is (= 200 (:status response)))
      (is (= "executed" (:type body)))
      (is (= 2 (:valid body)))
      (is (= 0 (:invalid body)))
      (is (empty? (:errors body)))))))

(deftest import-activities-with-errors-test
  (when @database-available?
    (testing "handles CSV with invalid rows"
    (let [csv-content "Date,Activity,Activity type,Unit,Amount planned
2024-01-15,Test Activity,Type A,kg,100.5
,Invalid Activity,,,invalid
2024-01-15,Valid Activity,Type B,m,200.0"
          csv-file (aux/create-csv-file csv-content)
          handler (:handler (:router @test-system))
          request (aux/create-multipart-request csv-file "/api/import")
          response (aux/handler-request handler request)
          body (json/read-str (:body response) :key-fn keyword)]
      (is (= 200 (:status response)))
      (is (= "planned" (:type body)))
      (is (= 2 (:valid body)))
      (is (= 1 (:invalid body)))
      (is (= 1 (count (:errors body))))))))

(deftest import-missing-file-test
  (when @database-available?
    (testing "returns error when file is not provided"
    (let [handler (:handler (:router @test-system))
          request {:request-method :post
                   :uri "/api/import"
                   :multipart-params {}}
          response (aux/handler-request handler request)
          body (json/read-str (:body response) :key-fn keyword)]
      (is (= 400 (:status response)))
      (is (some? (:error body)))
      (is (= "CSV file not sent" (:error body)))))))

(deftest import-planned-and-executed-test
  (when @database-available?
    (testing "imports both planned and executed activities for same date"
    (let [handler (:handler (:router @test-system))
          
          ;; Import planned activities
          planned-csv "Date,Activity,Activity type,Unit,Amount planned
2024-01-15,Test Activity,Type A,kg,100.0"
          planned-file (aux/create-csv-file planned-csv)
          planned-request (aux/create-multipart-request planned-file "/api/import")
          planned-response (aux/handler-request handler planned-request)
          planned-body (json/read-str (:body planned-response) :key-fn keyword)]
      
      (is (= 200 (:status planned-response)))
      (is (= "planned" (:type planned-body)))
      (is (= 1 (:valid planned-body)))
      
      ;; Import executed activities
      (let [executed-csv "Date,Activity,Activity type,Unit,Amount executed
2024-01-15,Test Activity,Type A,kg,80.0"
            executed-file (aux/create-csv-file executed-csv)
            executed-request (aux/create-multipart-request executed-file "/api/import")
            executed-response (aux/handler-request handler executed-request)
            executed-body (json/read-str (:body executed-response) :key-fn keyword)]
        
        (is (= 200 (:status executed-response)))
        (is (= "executed" (:type executed-body)))
        (is (= 1 (:valid executed-body)))
        
        ;; Query activities to verify both amounts are present
        (let [query-request (aux/create-query-request "/api/activities" {"date" "2024-01-15"})
              query-response (aux/handler-request handler query-request)
              query-body (json/read-str (:body query-response) :key-fn keyword)]
          (is (= 200 (:status query-response)))
          (is (= 1 (count (:items query-body))))
          (let [activity (first (:items query-body))]
            (is (= "Test Activity" (:activity activity)))
            (is (= "executed" (:kind activity)))
            (is (= 80.0M (normalize-amount (:amount activity)))))))))))

(deftest import-updates-existing-activity-test
  (when @database-available?
    (testing "updates existing activity when importing same activity again"
    (let [handler (:handler (:router @test-system))
          
          ;; First import
          first-csv "Date,Activity,Activity type,Unit,Amount planned
2024-01-15,Test Activity,Type A,kg,100.0"
          first-file (aux/create-csv-file first-csv)
          first-request (aux/create-multipart-request first-file "/api/import")
          first-response (aux/handler-request handler first-request)]
      
      (is (= 200 (:status first-response)))
      
      ;; Second import with different amount
      (let [second-csv "Date,Activity,Activity type,Unit,Amount planned
2024-01-15,Test Activity,Type A,kg,150.0"
            second-file (aux/create-csv-file second-csv)
            second-request (aux/create-multipart-request second-file "/api/import")
            second-response (aux/handler-request handler second-request)
            second-body (json/read-str (:body second-response) :key-fn keyword)]
        
        (is (= 200 (:status second-response)))
        (is (= 1 (:valid second-body)))
        
        ;; Query to verify updated amount
        (let [query-request (aux/create-query-request "/api/activities" {"date" "2024-01-15"})
              query-response (aux/handler-request handler query-request)
              query-body (json/read-str (:body query-response) :key-fn keyword)]
          (is (= 200 (:status query-response)))
          (is (= 1 (count (:items query-body))))
          (let [activity (first (:items query-body))]
            (is (= "Test Activity" (:activity activity)))
            (is (= 150.0M (normalize-amount (:amount activity)))))))))))
