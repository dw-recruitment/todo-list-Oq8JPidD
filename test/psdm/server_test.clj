(ns psdm.server-test
  (:require [clojure.test :refer :all]
            [psdm.server :refer [handler app]]
            [psdm.items :as items]
            [ring.mock.request :as mock]
            [clojure.string :as str]
            [psdm.test-helper :as helper]
            [psdm.todo-list :as todo-list]
            [clojure.edn :as edn]))

(helper/system-fixture)

(defn content-type [resp]
  (get-in resp [:headers "Content-Type"]))

(deftest test-page-routes
  (testing "pages have a proper status and content type"
    (doseq [path ["/about" "/"]]
      (let [resp (app (mock/request :get path))]
        (is (= 200 (:status resp)))
        (is (str/starts-with?
              (content-type resp)
              "text/html")))))
  (testing "non-existent pages return a 404"
    (is (= 404 (:status (app (mock/request :get "/some-path/that/does/not/exist")))))))

(deftest test-asset-headers
  (testing "images have the appropriate content type"
    (is (str/starts-with?
          (-> (mock/request :get "/assets/images/netnow3.gif")
              app
              content-type)
          "image/gif"))))

(deftest test-api-list-todo-lists
  (let [todo-list (todo-list/create (helper/get-db) {:name "Bob's Todo List"})
        resp (app (mock/request :get "/api/"))]
    (testing "it returns a success status"
      (is (= 200 (:status resp))))
    (let [body (edn/read-string (:body resp))]
      (testing "it returns edn representing a list of todo lists"
        (is (coll? body))
        (is (= 1 (count body)))
        (is (= (:name todo-list) (first (map :name body)))))
      (testing "it contains self links"
        (is (= (str "/api/" (:id todo-list)) (first (map :self body))))))))

(deftest test-api-delete-todo-list
  (let [todo-list (todo-list/create (helper/get-db) {:name "Bob's Todo List"})
        resp (app (mock/request :delete (str "/api/" (:id todo-list))))]
    (testing "it returns success status"
      (is (= 204 (:status resp))))
    (testing "it deletes the todo-list"
      (is (not (todo-list/find-by-id (helper/get-db) (:id todo-list)))))))

(deftest test-api-get-todo-list
  (let [todo-list (todo-list/create (helper/get-db)
                                    {:name "Test List"})
        item (items/create (helper/get-db)
                           {:description  "descriptive"
                            :status       :todo
                            :todo_list_id (:id todo-list)})
        resp (app (mock/request :get (str "/api/" (:id todo-list))))]
    (testing "it returns success status"
      (is (= 200 (:status resp))))
    (let [body (edn/read-string (:body resp))]
      (is (= (:name todo-list) (:name body)))
      (is (coll? (:items body)))
      (is (= (:id item) (first (map :id (:items body)))))
      (is (= (str "/api/" (:id todo-list) "/" (:id item))
             (first (map :self (:items body))))))))

(deftest test-api-put-todo-item
  (let [todo-list (todo-list/create (helper/get-db)
                                    {:name "Test List"})
        item (items/create (helper/get-db)
                           {:description  "descriptive"
                            :status       :todo
                            :todo_list_id (:id todo-list)})
        item-uri (str "/api/" (:id todo-list) "/" (:id item))
        resp (app (-> (mock/request :put item-uri)
                      (mock/body
                        (pr-str {:description "descriptive"
                                 :self        item-uri
                                 :status      :done}))
                      (mock/content-type "application/edn")))
        body (edn/read-string (:body resp))]
    (is (= 200 (:status resp)))
    (is (= (:id item) (:id body)))
    (is (= :done (:status body)))
    (is (= :done (:status (items/find-by-id (helper/get-db) (:id item)))))))

(deftest test-api-create-todo-item
  (let [todo-list (todo-list/create (helper/get-db)
                                    {:name "Test List"})
        todo-list-uri (str "/api/" (:id todo-list))
        resp (app (-> (mock/request :post todo-list-uri)
                      (mock/body
                        (pr-str {:description "an item"}))
                      (mock/content-type "application/edn")))
        body (edn/read-string (:body resp))]
    (is (= 201 (:status resp)))
    (is (= "an item" (:description body)))
    (is (:self body))))

(deftest test-api-delete-todo-item
  (let [todo-list (todo-list/create (helper/get-db)
                                    {:name "Test List"})
        item (items/create (helper/get-db)
                           {:description  "descriptive"
                            :status       :todo
                            :todo_list_id (:id todo-list)})
        item-uri (str "/api/" (:id todo-list) "/" (:id item))
        resp (app (mock/request :delete item-uri))]
    (is (= 204 (:status resp)))
    (is (not (items/find-by-id (helper/get-db) (:id item))))))
