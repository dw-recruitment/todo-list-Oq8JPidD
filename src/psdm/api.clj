(ns psdm.api
  (:require [ring.util.response :as resp]
            [psdm.todo-list :as todo-list]
            [psdm.items :as items]))

(defn generate-response [content & [status]]
  {:status  (or status 200)
   :headers {"Content-Type" "application/edn"}
   :body    (pr-str content)})

(defn get-db [req]
  (get-in req [:components :db]))

(defn get-todo-list-id [req]
  (-> req
      :route-params
      :todo-list-id
      (Integer/parseInt)))

(defn get-todo-item-id [req]
  (-> req
      :route-params
      :todo-item-id
      (Integer/parseInt)))

(defn strip-audit-times [m]
  (dissoc m :created_at :updated_at))

(defn post-todo-item [req]
  (let [todo-list-id (get-todo-list-id req)
        item (assoc (:edn-params req)
               :todo_list_id todo-list-id)
        created (items/create (get-db req) item)
        self-uri (str (:uri req) "/" (:id created))]
    (-> created
        strip-audit-times
        (assoc :self self-uri)
        (generate-response 201)
        (assoc-in [:headers "Location"] self-uri))))

(defn post-todo-list [req]
  (let [todo-list (todo-list/create (get-db req) (:edn-params req))
        self-uri (str (:uri req) (:id todo-list))]
    (-> todo-list
        strip-audit-times
        (assoc :self self-uri)
        (generate-response 201)
        (assoc-in [:headers "Location"] self-uri))))

(defn put-todo-item [req]
  (let [item (items/update (get-db req)
                           (-> (:edn-params req)
                               (dissoc (:edn-params req) :self)
                               (assoc :id (get-todo-item-id req))))]
    (generate-response (assoc (strip-audit-times item)
                         :self (:uri req)))))

(defn delete-todo-item [req]
  (let [item-id (get-todo-item-id req)]
    (items/delete (get-db req) item-id)
    {:status  204
     :headers {}
     :body    ""}))

(defn get-todo-list [req]
  (let [todo-list (todo-list/find-by-id (get-db req)
                                        (get-todo-list-id req))
        items (->> (items/find-all-for-list (get-db req)
                                            (:id todo-list))
                   (map strip-audit-times)
                   (map (fn [item]
                          (assoc item :self (str (:uri req) "/" (:id item))))))]
    (generate-response {:name  (:name todo-list)
                        :self  (:uri req)
                        :items (into [] items)})))

(defn delete-todo-list [req]
  (let [id (get-todo-list-id req)]
    (todo-list/delete (get-db req) id)
    {:status  204
     :headers {}
     :body    ""}))

(defn list-todo-lists [req]
  (generate-response
    (->> (todo-list/find-all (get-db req) {})
         (map strip-audit-times)
         (map (fn [item]
                (assoc item :self (str (:uri req) (:id item)))))
         (into []))))
