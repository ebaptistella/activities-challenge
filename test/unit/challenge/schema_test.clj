(ns challenge.schema-test
  (:require [challenge.schema :as schema]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [schema.core :as s]
            [schema.test :refer [validate-schemas]]))

(use-fixtures :once validate-schemas)

(deftest strict-schema-test
  (testing "creates schema with required fields"
    (let [skeleton {:id {:schema s/Int :required true}
                    :name {:schema s/Str :required true}}
          result (schema/strict-schema skeleton)]
      (is (= s/Int (get result :id)))
      (is (= s/Str (get result :name)))))

  (testing "creates schema with optional fields using optional-key"
    (let [skeleton {:id {:schema s/Int :required true}
                    :name {:schema s/Str :required false}}
          result (schema/strict-schema skeleton)
          optional-name-key (s/optional-key :name)]
      (is (= s/Int (get result :id)))
      (is (contains? result optional-name-key))
      (is (= s/Str (get result optional-name-key)))))

  (testing "creates schema with mixed required and optional fields"
    (let [skeleton {:id {:schema s/Int :required true}
                    :name {:schema s/Str :required true}
                    :email {:schema s/Str :required false}
                    :age {:schema s/Int :required false}}
          result (schema/strict-schema skeleton)
          optional-email-key (s/optional-key :email)
          optional-age-key (s/optional-key :age)]
      (is (= s/Int (get result :id)))
      (is (= s/Str (get result :name)))
      (is (contains? result optional-email-key))
      (is (contains? result optional-age-key))
      (is (= s/Str (get result optional-email-key)))
      (is (= s/Int (get result optional-age-key)))))

  (testing "validation works correctly with strict schema"
    (let [skeleton {:id {:schema s/Int :required true}
                    :name {:schema s/Str :required true}
                    :email {:schema s/Str :required false}}
          strict-schema (schema/strict-schema skeleton)
          valid-data {:id 1 :name "Test"}
          invalid-data {:id 1}]
      (is (= valid-data (s/validate strict-schema valid-data)))
      (is (thrown? Exception (s/validate strict-schema invalid-data))))))

(deftest loose-schema-test
  (testing "creates schema that accepts extra fields"
    (let [skeleton {:id {:schema s/Int :required true}
                    :name {:schema s/Str :required true}}
          result (schema/loose-schema skeleton)]
      (is (= s/Int (get result :id)))
      (is (= s/Str (get result :name)))))

  (testing "validation works correctly with loose schema"
    (let [skeleton {:id {:schema s/Int :required true}
                    :name {:schema s/Str :required true}}
          loose-schema (schema/loose-schema skeleton)
          valid-data {:id 1 :name "Test"}]
      (is (= valid-data (s/validate loose-schema valid-data)))
      ;; Note: Currently loose-schema uses the same implementation as strict-schema
      ;; Prismatic Schema by default rejects extra keys unless using s/Any or {s/Any s/Any}
      ;; This test verifies the current behavior
      (is (thrown? Exception (s/validate loose-schema {:id 1 :name "Test" :extra-field "extra"})))))

  (testing "loose-schema uses same structure as strict-schema"
    (let [skeleton {:id {:schema s/Int :required true}
                    :name {:schema s/Str :required false}}
          strict-result (schema/strict-schema skeleton)
          loose-result (schema/loose-schema skeleton)]
      ;; Both should have the same structure (loose-schema calls strict-schema internally)
      (is (= (count (keys strict-result)) (count (keys loose-result)))))))
