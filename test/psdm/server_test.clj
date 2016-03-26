(ns psdm.server-test
  (:require [clojure.test :refer :all]
            [psdm.server :refer [handler app
                                 todo-items-base-path
                                 todo-item-params]]
            [psdm.items :as items]
            [schema.utils :as u]
            [ring.mock.request :as mock]
            [clojure.string :as str]
            [psdm.test-helper :as helper])
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

(deftest test-todo-items-form
  (let [description (str (UUID/randomUUID))
        resp (->> {"description" description}
                  (mock/request :post todo-items-base-path)
                  app)]
    ;; 303 is correct here. 302 is commonly used, but technically not correct
    ;; as a 302 to requires the same request method be used when accessing the
    ;; URL in the location header. Browsers don't, for good reason, implement
    ;; that part of the spec correctly because it would break the web.
    (testing "it returns a 303 to redirect the browser"
      (is (= 303 (:status resp))))
    (testing "it redirects back to the todo items list"
      (is (#{todo-items-base-path} (get-in resp [:headers "Location"]))))
    (testing "it creates a todo_item on a post"
      (let [matching-items (->> (items/find-all (helper/get-db) {})
                                (filter (comp (partial = description)
                                              :description)))]
        (is (= 1 (count matching-items)))
        (testing "with a status of :todo"
          (is (= :todo (:status (first matching-items))))))))
  (let [resp (->> {"description" "a description"
                   "created_at" "2016-03-26 12:00:00"}
                  (mock/request :post todo-items-base-path)
                  app)]
    (testing "it returns a 400 if the payload is invalid"
      (is (= 400 (:status resp))))))

(deftest test-todo-item-form-params
  (let [form-params {"created_at"  "should be ignored"
                     "description" "a description"
                     "status"      "todo"}]
    (testing "it only accepts :description and :status"
      (is (u/error? (todo-item-params form-params))))
    (testing "it coerces values properly"
      (let [parsed-value (todo-item-params (dissoc form-params
                                                   "created_at"))]
        (is (= :todo (:status parsed-value)))))))
