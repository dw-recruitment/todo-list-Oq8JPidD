(ns psdm.items
  (:require [honeysql.core :as sql]
    ;; Once upon a time, update was not part of clojure.core. Then, one day,
    ;; someone added it there. Now every library in Clojureland elicits a
    ;; warning message about 'update' being shadowed. The end.
            [honeysql.helpers :exclude [update] :refer :all :as sql-h]
            [clojure.set :as set]
            [clojure.java.jdbc :as jdbc]
            [clj-time.core :as dt]
            [psdm.dao :as dao])
  (:refer-clojure :exclude [update]))

(defn build-empty-item []
  {:status :todo})

(def integer->status {0 :todo
                      1 :done})

(def status->integer (set/map-invert integer->status))

(defn deserialize [db-item]
  (when db-item
    (-> db-item
        dao/deserialize-audit-dates
        (clojure.core/update :status integer->status))))

(defn serialize [item]
  (when item
    (-> item
        dao/serialize-audit-dates
        (clojure.core/update :status status->integer))))

(defn find-all [db opts]
  (map deserialize (dao/find-all db :todo_items opts)))

(defn find-by-id [db id]
  (deserialize (dao/find-by-id db :todo_items id)))

(defn create [db item]
  (let [item (merge (build-empty-item) item)
        item (serialize item)]
    (deserialize (dao/create db :todo_items item))))

(defn update [db item]
  (let [item (assoc item :updated_at (dt/now))
        sql-and-params (-> (sql-h/update :todo_items)
                           (sset (serialize (dissoc item :id)))
                           (where [:= :id :?id])
                           (sql/format :params {:id (:id item)}))]
    (jdbc/with-db-transaction [tx db]
      (jdbc/execute! tx sql-and-params)
      (find-by-id tx (:id item)))))

(defn upsert [db item]
  (if (:id item)
    (update db item)
    (create db item)))

(defn delete [db id]
  (let [sql-and-params (-> (delete-from :todo_items)
                           (where [:= :id :?id])
                           (sql/format :params {:id id}))]
    (jdbc/with-db-transaction [tx db]
      (jdbc/execute! tx sql-and-params))))
