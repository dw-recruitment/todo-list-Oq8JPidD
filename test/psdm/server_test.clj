(ns psdm.server-test
  (:require [clojure.test :refer :all]
            [psdm.server :refer [handler app
                                 todo-item-params
                                 todo-list-url]]
            [psdm.items :as items]
            [schema.utils :as u]
            [ring.mock.request :as mock]
            [clojure.string :as str]
            [psdm.test-helper :as helper]
            [psdm.todo-list :as todo-list]
            [clojure.edn :as edn])
  (:import [java.util UUID]))

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

#_(deftest test-todo-items-form
  (let [todo-list (todo-list/create (helper/get-db)
                                    {:name "Test List"})
        todo-list-url (todo-list-url (:id todo-list))
        description (str (UUID/randomUUID))
        resp (->> {"description" description}
                  (mock/request :post todo-list-url)
                  app)]
    ;; 303 is correct here. 302 is commonly used, but technically not correct
    ;; as a 302 to requires the same request method be used when accessing the
    ;; URL in the location header. Browsers don't, for good reason, implement
    ;; that part of the spec correctly because it would break the web.
    (testing "it returns a 303 to redirect the browser"
      (is (= 303 (:status resp))))
    (testing "it redirects back to the todo items list"
      (is (= todo-list-url (get-in resp [:headers "Location"]))))
    (testing "it creates a todo_item on a post"
      (let [matching-items (->> (items/find-all (helper/get-db) {})
                                (filter (comp (partial = description)
                                              :description)))]
        (is (= 1 (count matching-items)))
        (testing "with a status of :todo"
          (is (= :todo (:status (first matching-items))))))))
  (let [todo-list (todo-list/create (helper/get-db)
                                    {:name "Test List"})
        todo-list-url (todo-list-url (:id todo-list))
        resp (->> {"description" "a description"
                   "created_at"  "2016-03-26 12:00:00"}
                  (mock/request :post todo-list-url)
                  app)]
    (testing "it returns a 400 if the payload is invalid"
      (is (= 400 (:status resp))))))

#_(deftest test-delete-todo-item
  (let [todo-list (todo-list/create (helper/get-db)
                                    {:name "Test List"})
        item (items/create (helper/get-db)
                           {:description  "descriptive"
                            :status       :todo
                            :todo_list_id (:id todo-list)})
        req (mock/request :post (todo-list-url (:id todo-list))
                          {"id"      (str (:id item))
                           "_method" "delete"})
        resp (app req)]
    (testing "it returns a 303 to redirect the browser"
      (is (= 303 (:status resp))))
    (testing "it redirects back to the todo items list"
      (is (= (todo-list-url (:id todo-list))
             (get-in resp [:headers "Location"]))))
    (testing "it deletes the indicated todo item"
      (is (not (items/find-by-id (helper/get-db) (:id item)))))))

#_(deftest test-todo-item-form-params
  (let [form-params {"created_at"  "should be ignored"
                     "description" "a description"
                     "status"      "todo"}]
    (testing "it only accepts :description and :status"
      (is (u/error? (todo-item-params form-params))))
    (testing "it coerces values properly"
      (let [parsed-value (todo-item-params (-> form-params
                                               (dissoc "created_at")
                                               (assoc "id" "5")))]
        (is (= :todo (:status parsed-value)))
        (is (= 5 (:id parsed-value)))))))

#_(deftest test-create-todo-list
  (let [list-name (str (UUID/randomUUID))
        resp (app (mock/request :post "/" {:name list-name}))]
    (testing "it returns a 303 to redirect the browser"
      (is (= 303 (:status resp))))
    (testing "it redirects back to the todo lists"
      (is (= "/" (get-in resp [:headers "Location"]))))
    (testing "it creates the todo list"
      (is (= 1 (->> (todo-list/find-all (helper/get-db) {})
                    (map :name)
                    (filter #{list-name})
                    count))))))

#_(deftest test-delete-todo-list
  (let [todo-list (todo-list/create (helper/get-db) {:name "Bob's Todo List"})
        resp (app (mock/request :post "/"
                                {"_method" "delete"
                                 "id"      (:id todo-list)}))]
    (testing "it returns a 303 to redirect the browser"
      (is (= 303 (:status resp))))
    (testing "it redirects back to the todo lists"
      (is (= "/" (get-in resp [:headers "Location"]))))
    (testing "it deletes the todo list"
      (is (not (todo-list/find-by-id (helper/get-db) (:id todo-list)))))))

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
