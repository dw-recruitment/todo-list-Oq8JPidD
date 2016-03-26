(ns psdm.items
  (:require [honeysql.core :as sql]
    ;; Once upon a time, update was not part of clojure.core. Then, one day,
    ;; someone added it there. Now every library in Clojureland elicits a
    ;; warning message about 'update' being shadowed. The end.
            [honeysql.helpers :exclude [update] :refer :all]
            [clojure.set :as set]
            [clojure.java.jdbc :as jdbc]))

(defn build-empty-item []
  {:status :todo})

(def list-defaults {:limit 100 :offset 0})

(def integer->status {0 :todo
                      1 :done})

(def status->integer (set/map-invert integer->status))

(defn deserialize [db-item]
  (update db-item :status integer->status))

(defn serialize [item]
  (update item :status status->integer))

(defn find-all [db opts]
  (let [opts (merge list-defaults opts)
        sql-and-params (-> (select :*)
                           (from :todo_items)
                           (order-by [:created_at :asc])
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
        sql-and-params (-> (insert-into :todo_items)
                           (values [(serialize item)])
                           sql/format)]
    (jdbc/with-db-transaction [tx db]
      (jdbc/execute! tx sql-and-params)
      (find-by-id tx (get-last-id tx)))))
