(ns psdm.test-helper
  (:require [clojure.test :refer [use-fixtures]]
            [psdm.config :as config]
            [psdm.migration :as migration]
            [kosmos]))

;; I'm not a fan of grabbing compontents out of some global state like this, but
;; it turns out to be rather convenient for tests. Maybe there is a better way
;; though?
(def system (atom nil))

(defn start-system [f]
  (binding [config/*env* "test"]
    (reset! system (config/start-system))
    (f)))

(defn stop-system [f]
  (f)
  (config/stop-system)
  (reset! system nil))

(defn setup-db [f]
  (let [migration (:migration @system)]
    (migration/migrate migration)
    (f)))

(defn teardown-db [f]
  (f)
  (migration/drop-db (:migration @system)))

(defn system-fixture []
  (use-fixtures :each start-system stop-system setup-db teardown-db))

(defn get-db []
  (:db @system))
