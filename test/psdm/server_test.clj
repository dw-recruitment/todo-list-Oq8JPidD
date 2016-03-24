(ns psdm.server-test
  (:require [clojure.test :refer :all]
            [psdm.server :refer [handler app]]
            [ring.mock.request :as mock]
            [clojure.string :as str]))

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
