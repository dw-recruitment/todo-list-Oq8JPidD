(ns psdm.items-test
  (:require [clojure.test :refer :all]
            [psdm.items :exclude [update] :refer :all :as items]
            [psdm.test-helper :as helper])
  (:import [java.util Date]))

(helper/system-fixture)

(deftest test-create
  (let [item (create (helper/get-db) {:description "A todo item todo"
                                      :status      :todo})]
    (is item)
    (testing "it gets an id"
      (is (:id item)))
    (testing "it has a status with keyword value"
      (is (= :todo (:status item))))))

(deftest test-update
  (let [item (create (helper/get-db) {:description "What an amazing thing todo"
                                      :status      :todo})
        id (:id item)]
    (items/update (helper/get-db)
                  (assoc item :status :done))
    (is (= :done (:status (find-by-id (helper/get-db) id))))))

(deftest test-upsert
  (let [item (create (helper/get-db) {:description "What an amazing thing todo"
                                      :status      :todo})
        id (:id item)]
    (testing "Given an item with an ID, it updates the existing item"
      (items/upsert (helper/get-db)
                    (assoc item :status :done))
      (is (= :done (:status (find-by-id (helper/get-db) id)))))
    (testing "Given an item without an ID, it creates a new one"
      (let [inserted-item (items/upsert (helper/get-db)
                                        (dissoc item :id))]
        (is (= :done (:status (find-by-id (helper/get-db) id))))
        (is (:id inserted-item))))))

(deftest test-find-all
  (let [items (->> (range 50)
                   (map (fn [i] {:description (str "item " i)
                                 :status      :todo}))
                   (map create (repeat (helper/get-db)))
                   (into #{}))
        retrieved-items (find-all (helper/get-db) {})]
    (is (= (count items) (count retrieved-items)))
    (is (= items (into #{} retrieved-items)))
    (testing "it returns the items sorted by the date they were created"
      (let [dates (map :created_at retrieved-items)]
        (is (->> (map compare dates (rest dates))
                 (every? (partial > 1))))))))
