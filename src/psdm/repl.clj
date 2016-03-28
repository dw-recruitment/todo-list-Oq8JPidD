(ns psdm.repl
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.pprint :refer [pprint]]
            [clojure.repl :refer :all]
            [clojure.test :as test]
            [kosmos :refer [system] :as kosmos]
            [psdm.config :refer [start-system stop-system] :as config]
            [clojure.tools.namespace.repl :refer [refresh refresh-all]]))

(defn go []
  (start-system))

(defn reset []
  (stop-system)
  (refresh :after 'psdm.repl/go))
