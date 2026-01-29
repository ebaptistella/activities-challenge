(defproject challenge "0.1.0-SNAPSHOT"
  :description "Volis Challenge"
  :url "https://github.com/ebaptistella/volis-challenge"
  :license {:name "MIT"
            :url "https://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.12.2"]]
  :main ^:skip-aot challenge.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
