(ns psdm.migration
  (:require [psdm.config :as config]
            [ragtime.jdbc]
            [ragtime.repl]
            [kosmos]))

(def migrations-resources-dir "migrations")

(defn ragtime-config [db]
  {:datastore  (ragtime.jdbc/sql-database db)
   :migrations (ragtime.jdbc/load-resources migrations-resources-dir)})

(defn migrate [migration]
  (let [config (-> migration
                   :db
                   ragtime-config)]
    (ragtime.repl/migrate config)))

(defn rollback [migration]
  (-> migration
      :db
      ragtime-config
      ragtime.repl/rollback))

(defn -main
  [& args]
  (let [command (first args)
        ;; TODO: starting the whole system here is rather undesirable, we need
        ;; a way to only start the part of the system necessary for the
        ;; migration component.
        system (config/start-system)
        migration (:migration system)]
    (try
      (case command
        "migrate" (migrate migration)
        "rollback" (rollback migration))
      (finally
        (kosmos/stop system)))))
