(ns psdm.migration
  (:require [psdm.config :as config]
            [clojure.java.io :as io]
            [clojure.java.jdbc :as jdbc]
            [ragtime.jdbc]
            [ragtime.repl]
            [kosmos]))

(def migrations-resources-dir "db/migrations")

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

(defn data-sql-file []
  (io/resource (str "db/data/" config/*env* ".sql")))

;; Unfortunately, this incredibly useful function is private. This is a hack so
;; that we can get access to it. Of course, this isn't ideal.
(def read-sql (var ragtime.jdbc/read-sql))

(defn populate [migration]
  (when (config/not-production?)
    (when-let [sql-file (data-sql-file)]
      (println "Loading data from" sql-file)
      (jdbc/with-db-connection [conn (:db migration)]
        (doseq [s (read-sql sql-file)]
          (jdbc/db-do-commands conn s))))))

(defn drop-db [migration]
  ;; the following check is particularly important here
  (when (config/not-production?)
    (println "Dropping database")
    (jdbc/with-db-connection [conn (:db migration)]
      ;; TODO: this is a very database specific statement, ideally
      ;; we would have a more general way to do this
      (jdbc/execute! conn ["DROP SCHEMA PUBLIC CASCADE"]))))

(defn create-db [migration]
  ;; this is a no-op since HSQLDB will automatically create the
  ;; database if it doesn't exist
  (println "Creating database"))

(defn rebuild [migration]
  (doto migration
    (drop-db)
    (create-db)
    (migrate)
    (populate)))

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
        "rollback" (rollback migration)
        "populate" (populate migration)
        "rebuild" (rebuild migration)
        "drop-db" (drop-db migration)
        "create-db" (create-db migration))
      (finally
        (kosmos/stop system)))))
