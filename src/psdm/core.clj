(ns psdm.core
  (:require [psdm.config :as config])
  (:gen-class))

(defn -main
  [& _]
  (let [system (config/start-system)]
    (clojure.pprint/pprint system)))
