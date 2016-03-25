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
                 [ragtime "0.5.3"]]
  :main ^:skip-aot psdm.core
  :target-path "target/%s"
  :aliases {"db" ["run" "-m" "psdm.migration"]}
  :profiles {:uberjar {:aot :all}
             :dev     {:dependencies [[ring/ring-mock "0.3.0"]]}})
