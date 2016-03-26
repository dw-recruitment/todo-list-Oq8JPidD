(ns psdm.server
  (:require [hiccup.core :refer :all]
            [hiccup.page :refer [html5]]
            [hiccup.util]
            [psdm.config :as config]
            [psdm.items :as items]
            [schema.core :as s]
            [schema.coerce :as coerce]
            [schema.utils :as u]
            [compojure.core :refer [routes GET POST]]
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
    [:head [:title "Productivity Self Delusion Machine"]]
    [:body
     [:h1 (hiccup.util/escape-html page-name)]
     contents]))

(defn item->list-item [item]
  [:li (hiccup.util/escape-html (str (name (:status item)) " -- " (:description item)))])

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

(def TodoItemCreateOrUpdate {:description             s/Str
                             (s/optional-key :status) (s/enum :todo :done)})

(defn keywordize-map [params]
  (reduce-kv (fn [acc k v]
               (assoc acc (keyword k)
                          v))
             {} params))

(def todo-item-params (comp (coerce/coercer TodoItemCreateOrUpdate
                                            coerce/json-coercion-matcher)
                            keywordize-map))

(defn invalid-html [req error]
  ;; This is pretty bare-bones currently. In general, this shouldn't happen
  ;; unless someone is bypassing the UI.
  (layout req "Invalid"))

(defn todo-items-post-handler [req]
  (let [db (get-in req [:components :db])
        parsed-todo-item (todo-item-params (:form-params req))]
    (if (u/error? parsed-todo-item)
      (do
        (log/warn "Invalid todo-item" (u/error-val parsed-todo-item))
        (resp/status (resp/response (invalid-html req parsed-todo-item))
                     400))
      (do (items/create db parsed-todo-item)
          ;; doing this old school where we reload the whole page on a post.
          (resp/redirect todo-items-base-path 303)))))

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
    (GET "/about" _ about)
    (route/not-found not-found)))

(def app (let [handler (-> handler
                           (resource/wrap-resource "public")
                           content-type/wrap-content-type
                           params/wrap-params)]
           (if (= "development" config/*env*)
             (reload/wrap-reload handler)
             handler)))

;; kosmos's ring jetty component does an ugly thing where it will actually
;; mutate our app var using the following function. I'm not a fan, but it does
;; provide us the needed hook to get needed components to our routes.
;;
;; Among other problems this causes, reloading this namespace whipes out the
;; mutation made by the jetty component. This means ring's devel-reload doesn't
;; work. Errr...
(defn init-app [app server-component]
  (fn [request]
    (let [request (assoc request
                    :components (select-keys server-component [:db]))]
      (app request))))
