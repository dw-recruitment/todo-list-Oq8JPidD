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
  (map deserialize (dao/find-all db :todo_items)))

(defn find-all-for-list [db todo-list-id]
  (map deserialize (dao/find-all db :todo_items {:todo_list_id todo-list-id})))

(defn find-by-id [db id]
  (deserialize (dao/find-by-id db :todo_items id)))

(defn create [db item]
  (let [item (merge (build-empty-item) item)
        item (serialize item)]
    (deserialize (dao/create db :todo_items item))))

(defn update [db item]
  (deserialize (dao/update db :todo_items (serialize item))))

(defn upsert [db item]
  (if (:id item)
    (update db item)
    (create db item)))

(defn delete [db id]
  (dao/delete db :todo_items id))
