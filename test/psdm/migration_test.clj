(ns psdm.migration-test
  (:require [clojure.test :refer :all]
            [psdm.migration :refer :all]
            [clojure.java.jdbc :as jdbc]
            [clojure.string :as str]
            [kosmos]))

(def test-settings {:hsqldb-server
                    {:kosmos/init :kosmos.server/HsqlDbServerComponent
                     :port        "9005"
                     :database    "mem:test"
                     :dbname      "test"}

                    :db
                    {:kosmos/init     :kosmos.db/DbComponent
                     :kosmos/requires [:hsqldb-server]
                     :classname       "org.hsqldb.jdbc.JDBCDriver"
                     :subprotocol     "hsqldb"
                     :protocol        "hsql"
                     :subname         "hsql://localhost:9005/test"
                     :host            "localhost"
                     :port            9005
                     :database        "test"
                     :user            "SA"
                     :password        ""}

                    :migration
                    {:kosmos/requires [:db]}})

(defn get-tables [db]
  (->> "SELECT table_name FROM INFORMATION_SCHEMA.TABLES"
       (jdbc/query db)
       (map :table_name)
       (map str/lower-case)
       (into #{})))

(deftest test-migrate-rollback
  (with-redefs [migrations-resources-dir "test-migrations"]
    (let [system (-> test-settings kosmos/map->system kosmos/start)
          migration (:migration system)
          db (:db migration)]
      (try
        (testing "migrate runs any new migrations"
          (is (not ((get-tables db) "foo")))
          (migrate migration)
          (is ((get-tables db) "foo")))
        (testing "rollback backs out last migration"
          (rollback migration)
          (is (not ((get-tables db) "foo"))))
        (finally
          (kosmos/stop system))))))
