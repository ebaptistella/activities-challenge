(defproject challenge "0.1.0-SNAPSHOT"
  :description "Volis Challenge"
  :url "https://github.com/ebaptistella/volis-challenge"
  :license {:name "MIT"
            :url "https://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.12.2"]
                 [com.stuartsierra/component "1.1.0"]
                 [ring/ring-core "1.12.2"]
                 [ring/ring-jetty-adapter "1.12.2"]
                 [metosin/reitit-ring "0.7.1"]
                 [org.postgresql/postgresql "42.7.3"]
                 [com.github.seancorfield/next.jdbc "1.3.981"]
                 [migratus/migratus "1.4.5"]
                 [clojure-tools/clojure-tools "1.0.0"]
                 [org.clojure/tools.logging "1.3.0"]
                 [org.apache.logging.log4j/log4j-api "2.23.1"]
                 [org.apache.logging.log4j/log4j-core "2.23.1"]
                 [org.apache.logging.log4j/log4j-slf4j2-impl "2.23.1"]
                 [org.clojure/clojurescript "1.11.132"]
                 [org.clojure/data.json "2.5.0"]
                 [org.clojure/data.csv "1.0.1"]
                 [reagent/reagent "1.2.0"]
                 [cljsjs/react "18.2.0-1"]
                 [cljsjs/react-dom "18.2.0-1"]]
  :plugins [[lein-cljsbuild "1.1.8"]
            [lein-shell "0.5.0"]]
  :source-paths ["src"]
  :resource-paths ["resources"]
  :clean-targets ^{:protect false} ["resources/public/js" "target"]
  :cljsbuild {:builds [{:id "app"
                        :source-paths ["src"]
                        :compiler {:main volis-challenge.ui.core
                                   :asset-path "/js/out"
                                   :output-to "resources/public/js/app.js"
                                   :output-dir "resources/public/js/out"
                                   :optimizations :advanced
                                   :pretty-print false}}]}
  :migratus {:store :database
             :migration-dir "resources/migrations"
             :db {:connection-uri (System/getenv "DATABASE_URL")}}
  :main ^:skip-aot challenge.core
  :target-path "target/%s"
  :aliases {"lint" ["shell" "clj-kondo" "--lint" "src" "--parallel"]
            "lint-fix" ["shell" "clj-kondo" "--copy-configs" "--lint" "src" "--parallel"]
            "check-all" ["do" ["check"] ["shell" "clj-kondo" "--lint" "src" "--parallel"]]
            "test-all" ["do" ["test"] ["lint"]]
            "clean-all" ["do" ["clean"] ["cljsbuild" "clean"]]
            "repl" ["with-profile" "+dev" "repl"]
            "uberjar-all" ["do" ["clean"] ["cljsbuild" "once" "app"] ["uberjar"]]}
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]
                       :prep-tasks [["compile"] ["cljsbuild" "once" "app"]]}
             :dev {}})
