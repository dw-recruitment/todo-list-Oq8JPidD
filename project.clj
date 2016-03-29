(defproject psdm "0.1.0-SNAPSHOT"
  :description "For when actually being productive is just too hard."
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [compojure "1.5.0"]
                 [kosmos "0.0.4"]
                 [kosmos/kosmos-web "0.0.2"]
                 [hiccup "1.0.5"]
                 [kosmos/kosmos-hsqldb-server "0.0.1"]
                 [kosmos/kosmos-hikari "0.0.1"]
                 [ring/ring-devel "1.4.0"]
                 [honeysql "0.6.3"]
                 [ragtime "0.5.3"]
                 [prismatic/schema "1.1.0"]
                 [clj-time "0.11.0"]

                 ;; cljs dependencies
                 [org.clojure/clojurescript "1.7.228" :scope "provided"]
                 [org.omcljs/om "1.0.0-alpha31"]
                 #_[cljsjs/bootstrap "3.3.6-0"]

                 ;; the logging API
                 [org.slf4j/slf4j-api "1.7.19"]
                 ;; this is an empty commons-logging artifact that will replace
                 ;; the version of this library coming from any of our
                 ;; dependencies. We could just exclude the artifact from each
                 ;; our dependencies, but someone will forget to do that, and
                 ;; only after many hours of painful debugging will we discover
                 ;; what happened. This way is less brittle.
                 [commons-logging/commons-logging "99-empty"]
                 ;; this is a reimplementation of commons-logging that uses
                 ;; slf4j. Basically, it allows us to use slf4j for libraries
                 ;; that were written for commons-logging.
                 [org.slf4j/jcl-over-slf4j "1.7.19"]
                 ;; this is to get java util logging to use slf4j, not that
                 ;; anyone actually uses that
                 [org.slf4j/jul-to-slf4j "1.7.19"]
                 ;; at long last, logback, a logging provider for slf4j
                 [ch.qos.logback/logback-classic "1.1.6"]
                 ]
  :main ^:skip-aot psdm.core
  :repl-options {:init-ns psdm.repl}
  :source-paths ["src" "src-cljs"]
  :test-paths ["test"]
  :target-path "target/%s"
  :clean-targets ^{:protect false} [:target-path :compile-path "resources/public/js/compiled"]
  :plugins [[lein-cloverage "1.0.6"]
            [lein-cljsbuild "1.1.1"]]
  :aliases {"db" ["run" "-m" "psdm.migration"]
            "db-manager" ["run" "-m" "org.hsqldb.util.DatabaseManagerSwing"
                          "--url" "jdbc:hsqldb:file:target/localdatabase"
                          "--user" "SA"]}
  ;; The version99 repo we need for an empty commons-logging artifact. This will
  ;; help us deal with the nightmare that is logging in java.
  :repositories [["version99" "http://version99.qos.ch/"]]
  :cljsbuild {:builds
              {:app
               {:source-paths ["src-cljs"]
                :compiler {:main psdm-client.core
                           :asset-path "/js/compiled/out"
                           :output-to "resources/public/js/compiled/app_main.js"
                           :output-dir "resources/public/js/compiled/out"
                           :source-map-timestamp true}}}}
  :profiles {:uberjar {:aot :all}
             :dev     {:dependencies [[ring/ring-mock "0.3.0"]]}})
