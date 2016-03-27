(ns psdm.server
  (:require [hiccup.core :refer :all]
            [hiccup.page :refer [html5 include-css]]
            [hiccup.util]
            [psdm.config :as config]
            [psdm.items :as items]
            [psdm.todo-list :as todo-list]
            [schema.core :as s]
            [schema.coerce :as coerce]
            [schema.utils :as u]
            [compojure.core :refer [routes context GET POST DELETE]]
            [compojure.route :as route]
            [clojure.tools.logging :as log]
            [ring.util.response :as resp]
            [ring.middleware.content-type :as content-type]
            [ring.middleware.resource :as resource]
            [ring.middleware.reload :as reload]
            [ring.middleware.params :as params]))

(defn todo-list-url [id]
  (str "/" id "/"))

(defn layout [req page-name & contents]
  (html5
    {:lang "en"}
    [:head
     (include-css "/style.css")
     [:title "Productivity Self Delusion Machine"]]
    [:body
     [:h1 (hiccup.util/escape-html page-name)]
     contents]))

(defn item-css-class [item]
  (get {:done "done-item"
        :todo "todo-item"}
       (:status item)
       ""))

(def opposite-status {:done :todo
                      :todo :done})

(def toggle-button-label
  {:todo "done"
   :done "undo"})

(defn item-doneness-toggle [item]
  [:form {:class "item-toggle"
          :action (todo-list-url (:todo_list_id item))
          :method "post"}
   [:input {:type  "hidden" :name "id"
            :value (:id item)}]
   [:input {:type  "hidden" :name "description"
            :value (:description item)}]
   [:input {:type  "hidden" :name "status"
            :value (name (opposite-status (:status item)))}]
   [:input {:type  "submit"
            :value (toggle-button-label (:status item))}]])

(defn item-delete-toggle [item]
  [:form {:class "item-toggle"
          :action (todo-list-url (:todo_list_id item))
          :method "post"}
   [:input {:type  "hidden" :name "_method"
            :value "delete"}]
   [:input {:type  "hidden" :name "id"
            :value (:id item)}]
   [:input {:type  "submit"
            :value "delete"}]])

(defn item->list-item [item]
  [:li
   (item-delete-toggle item)
   (item-doneness-toggle item)
   [:span {:class (item-css-class item)}
    (hiccup.util/escape-html (str (name (:status item)) " -- " (:description item)))]])

(defn create-todo-item-form [todo-list-id]
  [:div
   [:form {:action (todo-list-url todo-list-id) :method "post"}
    [:input {:type "text" :name "description" :maxlength 140}]
    [:input {:type "submit" :value "Submit"}]]])

(defn index-html [req todo-list items]
  (layout req "Productivity Self Delusion Machine"
          [:h2 (:name todo-list)]
          (create-todo-item-form (:id todo-list))
          [:ul
           (map item->list-item items)]))

(defn request->todo-list-id [req]
  (let [todo-list-id (-> req
                         (get-in [:route-params :todo-list-id])
                         Integer/parseInt)]
    todo-list-id))

(defn index [req]
  (let [db (get-in req [:components :db])
        todo-list-id (request->todo-list-id req)
        todo-list (todo-list/find-by-id db todo-list-id)
        items (items/find-all-for-list db todo-list-id)]
    (index-html req todo-list items)))

(def TodoItemCreateOrUpdate {(s/optional-key :id)     s/Int
                             :description             s/Str
                             (s/optional-key :status) (s/enum :todo :done)})

(defn keywordize-map [params]
  (reduce-kv (fn [acc k v]
               (assoc acc (keyword k)
                          v))
             {} params))

(def form-coercion-matcher (coerce/first-matcher
                             [coerce/string-coercion-matcher
                              coerce/json-coercion-matcher]))

(def todo-item-params (comp (coerce/coercer TodoItemCreateOrUpdate
                                            form-coercion-matcher)
                            keywordize-map))

(defn invalid-html [req error]
  ;; This is pretty bare-bones currently. In general, this shouldn't happen
  ;; unless someone is bypassing the UI.
  (layout req "Invalid"))

(defn todo-items-post-handler [req]
  (let [db (get-in req [:components :db])
        todo-list-id (request->todo-list-id req)
        ;; We don't set the todo_list_id via the form params. We want to control
        ;; this association based upon the URL posted to.
        item (assoc (todo-item-params (:form-params req))
               :todo_list_id todo-list-id)]
    (if (u/error? item)
      (do
        (log/warn "Invalid todo-item" (u/error-val item))
        (resp/status (resp/response (invalid-html req item))
                     400))
      (do (items/upsert db item)
          ;; doing this old school where we reload the whole page on a post.
          (resp/redirect (todo-list-url todo-list-id) 303)))))

(defn todo-items-delete-handler [req]
  ;; Not being all that RESTful here in this case as the URL does not identify
  ;; the resource we want to delete. We are using a form param for that purpose.
  ;; This is mainly a convenience thing. If we actually turn this into a more
  ;; general API we'll revisit.
  (let [id (-> req
               :form-params
               (get "id")
               (Integer/parseInt))
        todo-list-id (request->todo-list-id req)
        db (get-in req [:components :db])]
    (items/delete db id)
    (resp/redirect (todo-list-url todo-list-id) 303)))

(defn todo-list-delete-handler [req]
  (let [id (-> req
               :form-params
               (get "id")
               (Integer/parseInt))
        db (get-in req [:components :db])]
    (todo-list/delete db id)
    (resp/redirect "/" 303)))

(def TodoListCreateOrUpdate {(s/optional-key :id) s/Int
                             :name                s/Str})

(def todo-list-params (comp (coerce/coercer TodoListCreateOrUpdate
                                            form-coercion-matcher)
                            keywordize-map))

(defn create-todo-list-form []
  [:div
   [:form {:action "/" :method "post"}
    [:input {:type "text" :name "name" :maxlength 100}]
    [:input {:type "submit" :value "Submit"}]]])

(defn todo-list-post-handler [req]
  (let [db (get-in req [:components :db])
        todo-list (todo-list-params (:form-params req))]
    (if (u/error? todo-list)
      (do
        (log/warn "Invalid todo-list" (u/error-val todo-list))
        (resp/status (resp/response (invalid-html req todo-list))
                     400))
      (do (todo-list/create db todo-list)
          ;; doing this old school where we reload the whole page on a post.
          (resp/redirect "/" 303)))))

(defn todo-list->list-item [todo-list]
  [:li
   [:span
    [:a {:href (todo-list-url (:id todo-list))}
     (hiccup.util/escape-html (:name todo-list))]]])

(defn todo-list-index-html [req todo-lists]
  (layout req "Productivity Self Delusion Machine"
          [:h2 "TODO Lists"]
          (create-todo-list-form)
          [:ul
           (map todo-list->list-item todo-lists)]))

(defn todo-list-index [req]
  (let [db (get-in req [:components :db])
        todo-lists (todo-list/find-all db {})]
    (todo-list-index-html req todo-lists)))

(defn about [req]
  (layout req "About"
          [:p "Imagine a world in which you could write down the things you "
           "have to do... on a computer. Well, you don't have to imagine it "
           "any longer! The " [:em "Productivity Self Delusion Machine"]
           " enables you to "
           "to write down any task! Need to remember to vote? Make a task. "
           "What about doing the dishes? Make a task. And the best part, "
           "you get that feeling of accomplishment just from writing down "
           "that thing you should be doing right now. That's right, if you "
           "just keep writing down tasks, you can feel productive without "
           "having to do anything at all!"]))

(defn not-found [req]
  (layout req "Not Found!"))

(def todo-list-routes
  (routes
    (GET "/" _ todo-list-index)
    (POST "/" _ todo-list-post-handler)))

(defn todo-items-routes [todo-list-id]
  (routes
    (GET "/" _ index)
    (POST "/" _ todo-items-post-handler)
    (DELETE "/" _ todo-items-delete-handler)))

(def handler
  (routes
    (GET "/about" _ about)
    todo-list-routes
    (context "/:todo-list-id" [todo-list-id]
      (todo-items-routes todo-list-id))
    (route/not-found not-found)))

(defn components-request [request]
  ;; this isn't the best. We are reaching into kosmos's system var (mutated
  ;; on startup) to retrieve the web component and inject dependencies into the
  ;; request. Really, we should be given the components we need not looking them
  ;; up ourselves, but no time to fix this at the moment
  (assoc request :components (select-keys (:web kosmos/system) [:db])))

(defn wrap-components [handler]
  (fn [request]
    (handler (components-request request))))

(def app (let [handler (-> handler
                           (resource/wrap-resource "public")
                           content-type/wrap-content-type
                           params/wrap-params
                           wrap-components)]
           (if (= "development" config/*env*)
             (reload/wrap-reload handler)
             handler)))
