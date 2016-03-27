(ns psdm.dao
  (:require [honeysql.core :as sql]
            [honeysql.helpers :exclude [update] :refer :all :as sql-h]
            [clj-time.coerce :as coerce]
            [clj-time.core :as dt]
            [clojure.java.jdbc :as jdbc]))

(defn safe
  "Given a conversion function, returns a new conversion function such that
  if the original throws an exception, the passed value is returned instead."
  [f]
  (fn [x]
    (try
      (f x)
      (catch Exception e
        x))))

(defn update-audit-dates [m update-fn]
  (reduce (fn [m k]
            (if (and (contains? m k))
              (clojure.core/update m k (safe update-fn))
              m))
          m
          [:created_at :updated_at]))

(defn serialize-audit-dates [item]
  (update-audit-dates item coerce/to-date))

(defn deserialize-audit-dates [db-item]
  (update-audit-dates db-item coerce/from-date))

(defn get-last-id [conn]
  ;; this is how we get the last created id for hsqldb
  (->> "CALL IDENTITY()"
       (jdbc/query conn)
       ffirst
       second))

(defn find-by-id [db table id]
  (let [sql-and-params (-> (select :*)
                           (from table)
                           (where [:= :id :?id])
                           (sql/format :params {:id id}))]
    (deserialize-audit-dates (first (jdbc/query db sql-and-params)))))

(defn create [db table row]
  (let [now (dt/now)
        row (assoc row
              :created_at now
              :updated_at now)
        row (serialize-audit-dates row)
        sql-and-params (-> (insert-into table)
                           (values [row])
                           sql/format)]
    (jdbc/with-db-transaction [tx db]
      (jdbc/execute! tx sql-and-params)
      (find-by-id tx table (get-last-id tx)))))

(defn find-all [db table & criteria]
  (let [sql-map (-> (select :*)
                    (from table)
                    (order-by [:created_at :asc] :id))
        criteria (apply merge criteria)
        sql-map (reduce (fn [sql-map [k v]]
                          (merge-where sql-map [:= k v]))
                        sql-map
                        criteria)
        sql-and-params (sql/format sql-map)]
    ;; TODO: paginate
    (->> sql-and-params
         (jdbc/query db)
         (map deserialize-audit-dates))))

(defn delete [db table id]
  (let [sql-and-params (-> (delete-from table)
                           (where [:= :id id])
                           sql/format)]
    (jdbc/with-db-transaction [tx db]
      (jdbc/execute! tx sql-and-params))))
