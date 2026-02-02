(ns challenge.integration.activity-fake-test
  (:require [challenge.integration.aux.http-helpers :as http-helpers]
            [challenge.integration.aux.init :refer [defflow]]
            [clojure.test :refer [use-fixtures]]
            [schema.test :refer [validate-schemas]]
            [state-flow.api :refer [flow]]
            [state-flow.assertions.matcher-combinators :refer [match?]]))

(use-fixtures :once validate-schemas)

(defflow activity-fake-test
  (flow "test fake setup - health check"
        [response (http-helpers/request {:method :get :path "/health"})]
        (match? {:status 200
                 :body {:status "ok"
                        :service "challenge"}}
                response)))
