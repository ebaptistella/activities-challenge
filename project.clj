(defproject challenge "0.1.0-SNAPSHOT"
  :description ""
  :url "https://github.com/ebaptistella/challenge"
  :license {:name "MIT"
            :url "https://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.12.2"]
                 [com.stuartsierra/component "1.1.0"]
                 [org.postgresql/postgresql "42.7.3"]
                 [com.github.seancorfield/next.jdbc "1.3.981"]
                 [migratus/migratus "1.4.5"]
                 [ch.qos.logback/logback-classic "1.2.3" :exclusions [org.slf4j/slf4j-api]]
                 [org.slf4j/jul-to-slf4j "1.7.26"]
                 [org.slf4j/jcl-over-slf4j "1.7.26"]
                 [org.slf4j/log4j-over-slf4j "1.7.26"]
                 [io.pedestal/pedestal.service "0.5.8"]
                 [io.pedestal/pedestal.jetty "0.5.8"]
                 [prismatic/schema "1.4.1"]
                 [cheshire "5.11.0"]
                 [com.zaxxer/HikariCP "5.1.0"]
                 [clj-schema "0.5.1"]]
  :plugins [[com.github.clj-kondo/lein-clj-kondo "0.2.5"]
            [com.github.clojure-lsp/lein-clojure-lsp "2.0.13"]
            [lein-cljfmt "0.8.2"]
            [lein-nsorg "0.3.0"]]
  :clojure-lsp {:settings {:clean {:ns-inner-blocks-indentation :same-line}}}
  :source-paths ["src"]
  :test-paths ["test/unit" "test/integration"]
  :resource-paths ["resources"]
  :main challenge.main
  :aot [challenge.main]
  :migratus {:store :database
             :migration-dir "resources/migrations"
             :db {:connection-uri (System/getenv "DATABASE_URL")}}
  :profiles {:dev {:aliases {"run-dev" ["trampoline" "run" "-m" "challenge.main/-main"]}
                   :dependencies [[io.pedestal/pedestal.service-tools "0.5.8"]
                                  [nubank/matcher-combinators "3.8.3"]
                                  [nubank/mockfn "0.7.0"]
                                  [nubank/state-flow "5.20.0"]]}
             :repl-auto {:repl-options {:init-ns challenge.repl}}}
  :aliases {:repl ["with-profile" "+dev" "repl"]
            :repl-auto ["with-profile" "+dev,+repl-auto" "repl"]
            :uberjar-all ["do" ["clean"] ["cljsbuild" "once" "app"] ["uberjar"]]
            ;; clojure-lsp commands
            :clean-ns ["clojure-lsp" "clean-ns" "--dry"]
            :format ["clojure-lsp" "format" "--dry"]
            :diagnostics ["clojure-lsp" "diagnostics"]
            :clean-ns-fix ["clojure-lsp" "clean-ns"]
            :format-fix ["clojure-lsp" "format"]
            ;; cljfmt commands (code formatting)
            :cljfmt ["cljfmt" "check"]
            :cljfmt-fix ["cljfmt" "fix"]
            ;; nsorg commands (namespace require organization)
            ;; Note: nsorg shows diff by default, --replace applies changes
            :nsorg-check ["nsorg"]
            :nsorg-fix ["nsorg" "--replace"]
            ;; clj-kondo commands (static analysis)
            :kondo ["clj-kondo" "--lint" "src" "test"]
            ;; Combined lint commands
            :lint ["do" ["clean-ns"] ["format"] ["diagnostics"] ["cljfmt"] ["nsorg-check"] ["kondo"]]}
  :repl-options {:init-ns challenge.main})