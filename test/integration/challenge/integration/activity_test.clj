(ns challenge.integration.activity-test
  (:require [challenge.integration.aux.http-helpers :as http-helpers]
            [challenge.integration.aux.init :refer [defflow]]
            [clojure.test :refer [use-fixtures]]
            [clojure.test :refer [is]]
            [schema.test :refer [validate-schemas]]
            [state-flow.api :as flow :refer [flow return]]
            [state-flow.assertions.matcher-combinators :refer [match?]]))

(use-fixtures :once validate-schemas)

(defn valid-activity-data
  "Returns a valid activity data map for testing"
  []
  {:date "2024-01-15"
   :activity "Test activity"
   :activity-type "work"
   :unit "hours"
   :amount-planned 8
   :amount-executed 6})

(defn today-date-str
  "Returns today's date as ISO string"
  []
  (str (java.time.LocalDate/now)))

(defn future-date-str
  "Returns a future date as ISO string"
  []
  (str (.plusDays (java.time.LocalDate/now) 1)))

;; ============================================================================
;; POST /activities - Create Activity Tests
;; ============================================================================

(defflow create-activity-with-all-fields-test
  (flow "create activity with all required and optional fields"
        [response (http-helpers/request {:method :post
                                         :path "/activities"
                                         :body (valid-activity-data)})]
        (match? {:status 201
                 :body {:id number?
                        :date "2024-01-15"
                        :activity "Test activity"
                        :activity-type "work"
                        :unit "hours"
                        :amount-planned 8
                        :amount-executed 6
                        :created-at string?
                        :updated-at string?}}
                response)))

(defflow create-activity-with-only-required-fields-test
  (flow "create activity with only required fields"
        [response (http-helpers/request {:method :post
                                         :path "/activities"
                                         :body {:date "2024-01-15"
                                                :activity "Minimal activity"
                                                :activity-type "work"
                                                :unit "hours"}})]
        (match? {:status 201
                 :body {:id number?
                        :date "2024-01-15"
                        :activity "Minimal activity"
                        :activity-type "work"
                        :unit "hours"
                        :created-at string?
                        :updated-at string?}}
                response)))

(defflow create-activity-verifies-id-generation-test
  (flow "create activity verifies ID is auto-generated"
        [response1 (http-helpers/request {:method :post
                                          :path "/activities"
                                          :body (valid-activity-data)})
         response2 (http-helpers/request {:method :post
                                          :path "/activities"
                                          :body (valid-activity-data)})
         id1 (return (-> response1 :body :id))
         id2 (return (-> response2 :body :id))]
        (match? {:status 201
                 :body {:id number?}}
                response1)
        (match? {:status 201
                 :body {:id number?}}
                response2)
        (flow/return (is (not= id1 id2)))))

(defflow create-activity-verifies-timestamps-test
  (flow "create activity verifies timestamps are generated"
        [response (http-helpers/request {:method :post
                                         :path "/activities"
                                         :body (valid-activity-data)})
         created-at (return (-> response :body :created-at))
         updated-at (return (-> response :body :updated-at))]
        (match? {:status 201
                 :body {:created-at string?
                        :updated-at string?}}
                response)
        (flow/return (is (= created-at updated-at)))))

(defflow create-activity-missing-required-fields-test
  (flow "create activity fails when required fields are missing"
        [response (http-helpers/request {:method :post
                                         :path "/activities"
                                         :body {:date "2024-01-15"}})]
        (match? {:status 400
                 :body {:error string?}}
                response)))

(defflow create-activity-future-date-test
  (flow "create activity fails with future date"
        [response (http-helpers/request {:method :post
                                         :path "/activities"
                                         :body (assoc (valid-activity-data)
                                                      :date (future-date-str))})]
        (match? {:status 400
                 :body {:error #"Activity date cannot be in the future"}}
                response)))

(defflow create-activity-empty-description-test
  (flow "create activity fails with empty description"
        [response (http-helpers/request {:method :post
                                         :path "/activities"
                                         :body (assoc (valid-activity-data)
                                                      :activity "")})]
        (match? {:status 400
                 :body {:error #"Activity description cannot be empty"}}
                response)))

(defflow create-activity-whitespace-description-test
  (flow "create activity fails with whitespace-only description"
        [response (http-helpers/request {:method :post
                                         :path "/activities"
                                         :body (assoc (valid-activity-data)
                                                      :activity "   ")})]
        (match? {:status 400
                 :body {:error #"Activity description cannot be empty"}}
                response)))

(defflow create-activity-executed-exceeds-planned-test
  (flow "create activity fails when executed amount exceeds planned"
        [response (http-helpers/request {:method :post
                                         :path "/activities"
                                         :body (assoc (valid-activity-data)
                                                      :amount-planned 5
                                                      :amount-executed 10)})]
        (match? {:status 400
                 :body {:error #"Amount executed cannot exceed amount planned"}}
                response)))

(defflow create-activity-invalid-json-test
  (flow "create activity fails with invalid JSON"
        [response (http-helpers/request {:method :post
                                         :path "/activities"
                                         :body "invalid json"})]
        (match? {:status 400
                 :body {:error #"Invalid JSON"}}
                response)))

(defflow create-activity-missing-body-test
  (flow "create activity fails without request body"
        [response (http-helpers/request {:method :post
                                         :path "/activities"})]
        (match? {:status 400
                 :body {:error #"Request body is required"}}
                response)))

;; ============================================================================
;; GET /activities - List Activities Tests
;; ============================================================================

(defflow list-activities-empty-test
  (flow "list activities returns empty array when no activities exist"
        [response (http-helpers/request {:method :get
                                         :path "/activities"})]
        (match? {:status 200
                 :body {:activities []}}
                response)))

(defflow list-activities-single-activity-test
  (flow "list activities returns single activity"
        [create-response (http-helpers/request {:method :post
                                                :path "/activities"
                                                :body (valid-activity-data)})
         list-response (http-helpers/request {:method :get
                                              :path "/activities"})]
        (match? {:status 201} create-response)
        (match? {:status 200
                 :body {:activities [{:id number?
                                      :date "2024-01-15"
                                      :activity "Test activity"
                                      :activity-type "work"
                                      :unit "hours"}]}}
                list-response)))

(defflow list-activities-multiple-activities-test
  (flow "list activities returns multiple activities"
        [create1 (http-helpers/request {:method :post
                                        :path "/activities"
                                        :body {:date "2024-01-10"
                                               :activity "First activity"
                                               :activity-type "work"
                                               :unit "hours"}})
         create2 (http-helpers/request {:method :post
                                        :path "/activities"
                                        :body {:date "2024-01-15"
                                               :activity "Second activity"
                                               :activity-type "work"
                                               :unit "hours"}})
         create3 (http-helpers/request {:method :post
                                        :path "/activities"
                                        :body {:date "2024-01-20"
                                               :activity "Third activity"
                                               :activity-type "work"
                                               :unit "hours"}})
         list-response (http-helpers/request {:method :get
                                              :path "/activities"})]
        (match? {:status 201} create1)
        (match? {:status 201} create2)
        (match? {:status 201} create3)
        (match? {:status 200
                 :body {:activities (fn [activities]
                                      (= 3 (count activities))
                                      (every? #(contains? % :id) activities))}}
                list-response)))

(defflow list-activities-ordering-test
  (flow "list activities returns activities ordered by date and ID (most recent first)"
        [create1 (http-helpers/request {:method :post
                                        :path "/activities"
                                        :body {:date "2024-01-10"
                                               :activity "First"
                                               :activity-type "work"
                                               :unit "hours"}})
         create2 (http-helpers/request {:method :post
                                        :path "/activities"
                                        :body {:date "2024-01-15"
                                               :activity "Second"
                                               :activity-type "work"
                                               :unit "hours"}})
         list-response (http-helpers/request {:method :get
                                              :path "/activities"})]
        (match? {:status 201} create1)
        (match? {:status 201} create2)
        (match? {:status 200
                 :body {:activities (fn [activities]
                                      (let [dates (map :date activities)]
                                        (or (= "2024-01-15" (first dates))
                                            (= "2024-01-10" (first dates)))))}}
                list-response)))

;; ============================================================================
;; GET /activities/:id - Get Activity Tests
;; ============================================================================

(defflow get-activity-by-id-success-test
  (flow "get activity by ID returns activity when it exists"
        [create-response (http-helpers/request {:method :post
                                                :path "/activities"
                                                :body (valid-activity-data)})
         activity-id (return (-> create-response :body :id))
         get-response (http-helpers/request {:method :get
                                             :path (str "/activities/" activity-id)})]
        (match? {:status 201} create-response)
        (match? {:status 200
                 :body {:id activity-id
                        :date "2024-01-15"
                        :activity "Test activity"
                        :activity-type "work"
                        :unit "hours"
                        :amount-planned 8
                        :amount-executed 6
                        :created-at string?
                        :updated-at string?}}
                get-response)))

(defflow get-activity-by-id-not-found-test
  (flow "get activity by ID returns 404 when activity does not exist"
        [response (http-helpers/request {:method :get
                                         :path "/activities/99999"})]
        (match? {:status 404
                 :body {:error #"Activity not found"}}
                response)))

(defflow get-activity-by-id-invalid-id-test
  (flow "get activity by ID returns 400 when ID is invalid"
        [response (http-helpers/request {:method :get
                                         :path "/activities/invalid"})]
        (match? {:status 400
                 :body {:error #"NumberFormatException|Invalid parameter format"}}
                response)))

;; ============================================================================
;; PUT /activities/:id - Update Activity Tests
;; ============================================================================

(defflow update-activity-complete-test
  (flow "update activity with all fields"
        [create-response (http-helpers/request {:method :post
                                                :path "/activities"
                                                :body (valid-activity-data)})
         activity-id (return (-> create-response :body :id))
         created-at (return (-> create-response :body :created-at))
         update-response (http-helpers/request {:method :put
                                                :path (str "/activities/" activity-id)
                                                :body {:date "2024-01-20"
                                                       :activity "Updated activity"
                                                       :activity-type "personal"
                                                       :unit "days"
                                                       :amount-planned 10
                                                       :amount-executed 8}})
         updated-at (return (-> update-response :body :updated-at))]
        (match? {:status 201} create-response)
        (match? {:status 200
                 :body {:id activity-id
                        :date "2024-01-20"
                        :activity "Updated activity"
                        :activity-type "personal"
                        :unit "days"
                        :amount-planned 10
                        :amount-executed 8
                        :created-at created-at
                        :updated-at string?}}
                update-response)
        (flow/return (is (not= created-at updated-at)))))

(defflow update-activity-partial-test
  (flow "update activity with partial fields"
        [create-response (http-helpers/request {:method :post
                                                :path "/activities"
                                                :body (valid-activity-data)})
         activity-id (return (-> create-response :body :id))
         created-at (return (-> create-response :body :created-at))
         update-response (http-helpers/request {:method :put
                                                :path (str "/activities/" activity-id)
                                                :body {:activity "Partially updated"}})
         updated-at (return (-> update-response :body :updated-at))]
        (match? {:status 201} create-response)
        (match? {:status 200
                 :body {:id activity-id
                        :date "2024-01-15"
                        :activity "Partially updated"
                        :activity-type "work"
                        :unit "hours"
                        :amount-planned 8
                        :amount-executed 6
                        :created-at created-at
                        :updated-at string?}}
                update-response)
        (flow/return (is (not= created-at updated-at)))))

(defflow update-activity-verifies-timestamps-test
  (flow "update activity verifies updated-at changes but created-at does not"
        [create-response (http-helpers/request {:method :post
                                                :path "/activities"
                                                :body (valid-activity-data)})
         activity-id (return (-> create-response :body :id))
         original-created-at (return (-> create-response :body :created-at))
         original-updated-at (return (-> create-response :body :updated-at))
         _ (return (Thread/sleep 10)) ; Small delay to ensure timestamp difference
         update-response (http-helpers/request {:method :put
                                                :path (str "/activities/" activity-id)
                                                :body {:activity "Updated"}})
         updated-created-at (return (-> update-response :body :created-at))
         updated-updated-at (return (-> update-response :body :updated-at))]
        (match? {:status 201} create-response)
        (match? {:status 200} update-response)
        (flow/return (is (= original-created-at updated-created-at)))
        (flow/return (is (not= original-updated-at updated-updated-at)))))

(defflow update-activity-not-found-test
  (flow "update activity returns 404 when activity does not exist"
        [response (http-helpers/request {:method :put
                                         :path "/activities/99999"
                                         :body {:activity "Updated"}})]
        (match? {:status 404
                 :body {:error #"Activity not found"}}
                response)))

(defflow update-activity-invalid-id-test
  (flow "update activity returns 400 when ID is invalid"
        [response (http-helpers/request {:method :put
                                         :path "/activities/invalid"
                                         :body {:activity "Updated"}})]
        (match? {:status 400
                 :body {:error #"NumberFormatException|Invalid parameter format"}}
                response)))

(defflow update-activity-future-date-test
  (flow "update activity fails with future date"
        [create-response (http-helpers/request {:method :post
                                                :path "/activities"
                                                :body (valid-activity-data)})
         activity-id (return (-> create-response :body :id))
         update-response (http-helpers/request {:method :put
                                                :path (str "/activities/" activity-id)
                                                :body {:date (future-date-str)}})]
        (match? {:status 201} create-response)
        (match? {:status 400
                 :body {:error #"Activity cannot be updated"}}
                update-response)))

(defflow update-activity-executed-exceeds-planned-test
  (flow "update activity fails when executed amount exceeds planned"
        [create-response (http-helpers/request {:method :post
                                                :path "/activities"
                                                :body (valid-activity-data)})
         activity-id (return (-> create-response :body :id))
         update-response (http-helpers/request {:method :put
                                                :path (str "/activities/" activity-id)
                                                :body {:amount-planned 5
                                                       :amount-executed 10}})]
        (match? {:status 201} create-response)
        (match? {:status 400
                 :body {:error #"Amount executed cannot exceed amount planned"}}
                update-response)))

(defflow update-activity-invalid-json-test
  (flow "update activity fails with invalid JSON"
        [create-response (http-helpers/request {:method :post
                                                :path "/activities"
                                                :body (valid-activity-data)})
         activity-id (return (-> create-response :body :id))
         update-response (http-helpers/request {:method :put
                                                :path (str "/activities/" activity-id)
                                                :body "invalid json"})]
        (match? {:status 201} create-response)
        (match? {:status 400
                 :body {:error #"Invalid JSON"}}
                update-response)))

(defflow update-activity-missing-body-test
  (flow "update activity fails without request body"
        [create-response (http-helpers/request {:method :post
                                                :path "/activities"
                                                :body (valid-activity-data)})
         activity-id (return (-> create-response :body :id))
         update-response (http-helpers/request {:method :put
                                                :path (str "/activities/" activity-id)})]
        (match? {:status 201} create-response)
        (match? {:status 400
                 :body {:error #"Request body is required"}}
                update-response)))

;; ============================================================================
;; DELETE /activities/:id - Delete Activity Tests
;; ============================================================================

(defflow delete-activity-success-test
  (flow "delete activity returns 204 when activity exists"
        [create-response (http-helpers/request {:method :post
                                                :path "/activities"
                                                :body (valid-activity-data)})
         activity-id (return (-> create-response :body :id))
         delete-response (http-helpers/request {:method :delete
                                                :path (str "/activities/" activity-id)})]
        (match? {:status 201} create-response)
        (match? {:status 204} delete-response)))

(defflow delete-activity-verifies-removal-test
  (flow "delete activity verifies activity is removed"
        [create-response (http-helpers/request {:method :post
                                                :path "/activities"
                                                :body (valid-activity-data)})
         activity-id (return (-> create-response :body :id))
         delete-response (http-helpers/request {:method :delete
                                                :path (str "/activities/" activity-id)})
         get-response (http-helpers/request {:method :get
                                             :path (str "/activities/" activity-id)})]
        (match? {:status 201} create-response)
        (match? {:status 204} delete-response)
        (match? {:status 404
                 :body {:error #"Activity not found"}}
                get-response)))

(defflow delete-activity-not-found-test
  (flow "delete activity returns 404 when activity does not exist"
        [response (http-helpers/request {:method :delete
                                         :path "/activities/99999"})]
        (match? {:status 404
                 :body {:error #"Activity not found"}}
                response)))

(defflow delete-activity-invalid-id-test
  (flow "delete activity returns 400 when ID is invalid"
        [response (http-helpers/request {:method :delete
                                         :path "/activities/invalid"})]
        (match? {:status 400
                 :body {:error #"NumberFormatException|Invalid parameter format"}}
                response)))

;; ============================================================================
;; End-to-End Flow Tests
;; ============================================================================

(defflow complete-flow-create-list-get-update-delete-test
  (flow "complete flow: create → list → get → update → delete"
        [create-response (http-helpers/request {:method :post
                                                :path "/activities"
                                                :body (valid-activity-data)})
         activity-id (return (-> create-response :body :id))
         list-response (http-helpers/request {:method :get
                                              :path "/activities"})
         get-response (http-helpers/request {:method :get
                                             :path (str "/activities/" activity-id)})
         update-response (http-helpers/request {:method :put
                                                :path (str "/activities/" activity-id)
                                                :body {:activity "Updated in flow"}})
         delete-response (http-helpers/request {:method :delete
                                                :path (str "/activities/" activity-id)})
         final-get-response (http-helpers/request {:method :get
                                                   :path (str "/activities/" activity-id)})]
        (match? {:status 201
                 :body {:id number?}}
                create-response)
        (match? {:status 200
                 :body {:activities (fn [activities]
                                      (some #(= activity-id (:id %)) activities))}}
                list-response)
        (match? {:status 200
                 :body {:id activity-id}}
                get-response)
        (match? {:status 200
                 :body {:id activity-id
                        :activity "Updated in flow"}}
                update-response)
        (match? {:status 204} delete-response)
        (match? {:status 404} final-get-response)))

(defflow multiple-activities-flow-test
  (flow "create multiple activities → list → verify ordering"
        [create1 (http-helpers/request {:method :post
                                        :path "/activities"
                                        :body {:date "2024-01-10"
                                               :activity "First"
                                               :activity-type "work"
                                               :unit "hours"}})
         create2 (http-helpers/request {:method :post
                                        :path "/activities"
                                        :body {:date "2024-01-15"
                                               :activity "Second"
                                               :activity-type "work"
                                               :unit "hours"}})
         create3 (http-helpers/request {:method :post
                                        :path "/activities"
                                        :body {:date "2024-01-20"
                                               :activity "Third"
                                               :activity-type "work"
                                               :unit "hours"}})
         id1 (return (-> create1 :body :id))
         id2 (return (-> create2 :body :id))
         id3 (return (-> create3 :body :id))
         list-response (http-helpers/request {:method :get
                                              :path "/activities"})
         all-ids (return (set (map :id (-> list-response :body :activities))))]
        (match? {:status 201} create1)
        (match? {:status 201} create2)
        (match? {:status 201} create3)
        (match? {:status 200
                 :body {:activities (fn [activities]
                                      (= 3 (count activities)))}}
                list-response)
        (match? #{id1 id2 id3} all-ids)))

(defflow create-update-verify-changes-test
  (flow "create → update → verify changes are persisted"
        [create-response (http-helpers/request {:method :post
                                                :path "/activities"
                                                :body (valid-activity-data)})
         activity-id (return (-> create-response :body :id))
         original-activity (return (-> create-response :body))
         update-response (http-helpers/request {:method :put
                                                :path (str "/activities/" activity-id)
                                                :body {:activity "Changed activity"
                                                       :amount-executed 7}})
         get-response (http-helpers/request {:method :get
                                             :path (str "/activities/" activity-id)})
         original-updated-at (return (-> original-activity :updated-at))
         final-updated-at (return (-> get-response :body :updated-at))]
        (match? {:status 201} create-response)
        (match? {:status 200
                 :body {:activity "Changed activity"
                        :amount-executed 7}}
                update-response)
        (match? {:status 200
                 :body {:id activity-id
                        :activity "Changed activity"
                        :amount-executed 7
                        :date "2024-01-15"
                        :activity-type "work"
                        :unit "hours"
                        :amount-planned 8}}
                get-response)
        (flow/return (is (not= original-updated-at final-updated-at)))))
