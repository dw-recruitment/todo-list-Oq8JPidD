(ns psdm.server
  (:require [hiccup.core :refer :all]
            [hiccup.page :refer [html5 include-css]]
            [hiccup.util]
            [psdm.api :as api]
            [psdm.config :as config]
            [psdm.views.layout :as layout]
            [compojure.core :refer [routes context GET POST DELETE PUT]]
            [compojure.route :as route]
            [ring.middleware.content-type :as content-type]
            [ring.middleware.resource :as resource]
            [ring.middleware.reload :as reload]
            [ring.middleware.params :as params]
            [ring.middleware.edn :as ring-edn]))

(defn index [req]
  (layout/application))

(defn about [req]
  (layout/layout
    "About"
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
  (layout/layout "Not Found!"))

(def api-routes
  (routes
    (GET "/api/" _ api/list-todo-lists)
    (POST "/api/" _ api/post-todo-list)

    (GET "/api/:todo-list-id" _ api/get-todo-list)
    (POST "/api/:todo-list-id" _ api/post-todo-item)
    (DELETE "/api/:todo-list-id" _ api/delete-todo-list)

    (PUT "/api/:todo-list-id/:todo-item-id" _ api/put-todo-item)
    (DELETE "/api/:todo-list-id/:todo-item-id" _ api/delete-todo-item)))

(def handler
  (routes
    (GET "/" _ index)
    (GET "/about" _ about)
    api-routes
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
                           ring-edn/wrap-edn-params
                           wrap-components)]
           (if (= "development" config/*env*)
             (reload/wrap-reload handler)
             handler)))
