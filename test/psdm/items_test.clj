(ns psdm.items-test
  (:require [clojure.test :refer :all]
            [psdm.items :refer :all]
            [psdm.test-helper :as helper]))

(helper/db-fixture)

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
                   (into #{}))]
    (is (= (count items) (count (find-all (helper/get-db) {}))))
    (is (= items (into #{} (find-all (helper/get-db) {}))))))
