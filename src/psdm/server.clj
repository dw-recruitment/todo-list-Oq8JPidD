(ns psdm.server
  (:require [hiccup.core :refer :all]
            [hiccup.page :refer [html5 include-css]]
            [hiccup.util]
            [psdm.config :as config]
            [psdm.items :as items]
            [schema.core :as s]
            [schema.coerce :as coerce]
            [schema.utils :as u]
            [compojure.core :refer [routes GET POST DELETE]]
            [compojure.route :as route]
            [clojure.tools.logging :as log]
            [ring.util.response :as resp]
            [ring.middleware.content-type :as content-type]
            [ring.middleware.resource :as resource]
            [ring.middleware.reload :as reload]
            [ring.middleware.params :as params]))

(def todo-items-base-path "/")

(defn layout [req page-name & contents]
  (html5
    {:lang "en"}
    [:head
     (include-css "style.css")
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
  [:form {:class "item-toggle" :action todo-items-base-path :method "post"}
   [:input {:type "hidden" :name "id"
            :value (:id item)}]
   [:input {:type  "hidden" :name "description"
            :value (:description item)}]
   [:input {:type  "hidden" :name "status"
            :value (name (opposite-status (:status item)))}]
   [:input {:type  "submit"
            :value (toggle-button-label (:status item))}]])

(defn item-delete-toggle [item]
  [:form {:class "item-toggle" :action todo-items-base-path :method "post"}
   [:input {:type "hidden" :name "_method"
            :value "delete"}]
   [:input {:type "hidden" :name "id"
            :value (:id item)}]
   [:input {:type  "submit"
            :value "delete"}]])

(defn item->list-item [item]
  [:li
   (item-delete-toggle item)
   (item-doneness-toggle item)
   [:span {:class (item-css-class item)}
    (hiccup.util/escape-html (str (name (:status item)) " -- " (:description item)))]])

(defn create-todo-item-form []
  [:div
   [:form {:action todo-items-base-path :method "post"}
    [:input {:type "text" :name "description" :maxlength 140}]
    [:input {:type "submit" :value "Submit"}]]])

(defn index-html [req items]
  (layout req "Productivity Self Delusion Machine"
          [:h2 "For when actual productivity is just too hard"]
          (create-todo-item-form)
          [:ul
           (map item->list-item items)]))

(defn index [req]
  (let [db (get-in req [:components :db])
        items (items/find-all db {})]
    (index-html req items)))

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
        item (todo-item-params (:form-params req))]
    (if (u/error? item)
      (do
        (log/warn "Invalid todo-item" (u/error-val item))
        (resp/status (resp/response (invalid-html req item))
                     400))
      (do (items/upsert db item)
          ;; doing this old school where we reload the whole page on a post.
          (resp/redirect todo-items-base-path 303)))))

(defn todo-items-delete-handler [req]
  (let [id (-> req
               :form-params
               (get "id")
               (Integer/parseInt))
        db (get-in req [:components :db])]
    (items/delete db id)
    (resp/redirect todo-items-base-path 303)))

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

(def handler
  (routes
    (GET todo-items-base-path _ index)
    (POST todo-items-base-path _ todo-items-post-handler)
    (DELETE todo-items-base-path _ todo-items-delete-handler)
    (GET "/about" _ about)
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
