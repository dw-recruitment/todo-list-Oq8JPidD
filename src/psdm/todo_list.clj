(ns psdm.todo-list
  (:require [psdm.dao :as dao]))

(defn find-all [db opts]
  (dao/find-all db :todo_list opts))

(defn create [db todo-list]
  (dao/create db :todo_list todo-list))

(defn delete [db id])
