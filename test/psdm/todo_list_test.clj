(ns psdm.todo-list-test
  (:require [clojure.test :refer :all]
            [psdm.todo-list :refer :all]
            [psdm.test-helper :refer [get-db system-fixture]]
            [psdm.items :as items]))

(system-fixture)

(deftest test-create
  (let [item (create (get-db) {:name "A todo list of things todo"})]
    (is item)
    (testing "it gets an id"
      (is (:id item)))
    (testing "it has audit dates"
      (is (:created_at item))
      (is (:updated_at item)))))

(deftest test-find-all
  (let [todolists (->> (range 50)
                   (map (fn [i] {:name (str "todo list " i)}))
                   (map create (repeat (get-db)))
                   (into #{}))
        retrieved-lists (find-all (get-db) {})]
    (is (= (count todolists) (count retrieved-lists)))
    (is (= todolists (into #{} retrieved-lists)))
    (testing "it returns the items sorted by the date they were created"
      (let [dates (map :created_at retrieved-lists)]
        (is (->> (map compare dates (rest dates))
                 (every? (partial > 1))))))))

(deftest test-find-by-id
  (let [todolist (create (get-db) {:name "Test"})]
    (is (find-by-id (get-db) (:id todolist)))))

(deftest test-delete
  (let [todolist (create (get-db) {:name "Test"})
        item (items/create (get-db) {:description  "some item"
                                     :todo_list_id (:id todolist)})]
    (delete (get-db) (:id todolist))
    (is (not (find-by-id (get-db) (:id todolist))))
    (is (not (items/find-by-id (get-db) (:id item))))))
