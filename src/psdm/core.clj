(ns psdm.core
  (:require [clojure.edn :as edn]
            [psdm.config :as config]
            [kosmos])
  (:gen-class))

(defn -main
  [& _]
  ;; TODO: Make where configuration comes from configurable
  (let [system (->> (str "config/" config/*env* "/settings.edn")
                    slurp
                    edn/read-string
                    ;; build the components
                    kosmos/map->system
                    ;; start the system, this will add a shutdown hook to stop
                    ;; the system as well
                    kosmos/start)]
    (clojure.pprint/pprint system)))
