(defproject psdm "0.1.0-SNAPSHOT"
  :description "For when actually being productive is just too hard."
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [compojure "1.5.0"]
                 [kosmos "0.0.4"]
                 [kosmos/kosmos-hsqldb-server "0.0.1"]]
  :main ^:skip-aot psdm.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
