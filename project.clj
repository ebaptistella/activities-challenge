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
                 [io.pedestal/pedestal.jetty "0.5.8"]]
  :source-paths ["src"]
  :resource-paths ["resources" "config"]
  :migratus {:store :database
             :migration-dir "resources/migrations"
             :db {:connection-uri (System/getenv "DATABASE_URL")}}
  :profiles {:dev {:aliases {"run-dev" ["trampoline" "run" "-m" "challenge/-main"]}
                   :dependencies [[io.pedestal/pedestal.service-tools "0.5.8"]]
                   :main challenge.main}}
  :aliases {:repl ["with-profile" "+dev" "repl"]
            :uberjar-all ["do" ["clean"] ["cljsbuild" "once" "app"] ["uberjar"]]}
  :repl-options {:init-ns challenge.main})