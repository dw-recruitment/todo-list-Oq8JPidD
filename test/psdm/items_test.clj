(ns psdm.items-test
  (:require [clojure.test :refer :all]
            [psdm.items :refer :all]
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
