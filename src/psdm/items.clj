(ns psdm.items
  (:require [honeysql.core :as sql]
    ;; Once upon a time, update was not part of clojure.core. Then, one day,
    ;; someone added it there. Now every library in Clojureland elicits a
    ;; warning message about 'update' being shadowed. The end.
            [honeysql.helpers :exclude [update] :refer :all :as sql-h]
            [clojure.set :as set]
            [clojure.java.jdbc :as jdbc]
            [clj-time.core :as dt]
            [clj-time.coerce :as coerce])
  (:refer-clojure :exclude [update]))

(defn build-empty-item []
  {:status :todo})

(def list-defaults {:limit 100 :offset 0})

(def integer->status {0 :todo
                      1 :done})

(def status->integer (set/map-invert integer->status))

(defn update-audit-dates [m update-fn]
  (reduce (fn [m k]
            (if (contains? m k)
              (clojure.core/update m k update-fn)
              m))
          m
          [:created_at :updated_at]))

(defn serialize-audit-dates [item]
  (update-audit-dates item coerce/to-date))

(defn deserialize-audit-dates [db-item]
  (update-audit-dates db-item coerce/from-date))

(defn deserialize [db-item]
  (-> db-item
      deserialize-audit-dates
      (clojure.core/update :status integer->status)))

(defn serialize [item]
  (-> item
      serialize-audit-dates
      (clojure.core/update :status status->integer)))

(defn find-all [db opts]
  (let [opts (merge list-defaults opts)
        sql-and-params (-> (select :*)
                           (from :todo_items)
                           (order-by [:created_at :asc] :id)
                           sql/format)]
    ;; TODO: paginate
    (->> sql-and-params
         (jdbc/query db)
         (map deserialize))))

(defn get-last-id [conn]
  ;; this is how we get the last created id for hsqldb
  (->> "CALL IDENTITY()"
       (jdbc/query conn)
       ffirst
       second))

(defn find-by-id [db id]
  (let [sql-and-params (-> (select :*)
                           (from :todo_items)
                           (where [:= :id :?id])
                           (sql/format :params {:id id}))]
    (first (map deserialize
                (jdbc/query db sql-and-params)))))

(defn create [db item]
  (let [item (merge (build-empty-item) item)
        item (assoc item
               :created_at (dt/now)
               :updated_at (dt/now))
        sql-and-params (-> (insert-into :todo_items)
                           (values [(serialize item)])
                           sql/format)]
    (jdbc/with-db-transaction [tx db]
      (jdbc/execute! tx sql-and-params)
      (find-by-id tx (get-last-id tx)))))

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
