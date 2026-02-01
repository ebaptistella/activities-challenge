(ns challenge.interceptors.validation-test
  (:require [clojure.test :refer [deftest is testing]]
            [challenge.interceptors.validation :as validation]))

(deftest not-found-message?-test
  (testing "returns true for 'not found'"
    (is (true? (validation/not-found-message? "Activity not found"))))

  (testing "returns true for 'does not exist'"
    (is (true? (validation/not-found-message? "Activity does not exist"))))

  (testing "returns true for 'not exist'"
    (is (true? (validation/not-found-message? "Activity not exist"))))

  (testing "returns false for other messages"
    (is (false? (validation/not-found-message? "Invalid request")))
    (is (false? (validation/not-found-message? "Validation error")))
    (is (false? (validation/not-found-message? "Internal server error"))))

  (testing "returns false for nil"
    (is (false? (validation/not-found-message? nil))))

  (testing "case-insensitive matching"
    (is (true? (validation/not-found-message? "NOT FOUND")))
    (is (true? (validation/not-found-message? "Not Found")))
    (is (true? (validation/not-found-message? "not found")))
    (is (true? (validation/not-found-message? "DOES NOT EXIST")))
    (is (true? (validation/not-found-message? "Does Not Exist")))
    (is (true? (validation/not-found-message? "does not exist"))))

  (testing "matches partial strings"
    (is (true? (validation/not-found-message? "The activity was not found in the database")))
    (is (true? (validation/not-found-message? "Resource does not exist anymore")))
    (is (true? (validation/not-found-message? "Item not exist in system")))))
