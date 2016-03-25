(ns psdm.server
  (:require [hiccup.core :refer :all]
            [hiccup.page :refer [html5]]
            [psdm.config :as config]
            [compojure.core :refer [routes GET]]
            [compojure.route :as route]
            [ring.middleware.content-type :as content-type]
            [ring.middleware.resource :as resource]
            [ring.middleware.reload :as reload]))

(defn layout [req page-name & contents]
  (html5
    {:lang "en"}
    [:head [:title "Productivity Self Delusion Machine"]]
    [:body
     [:h1 page-name]
     contents]))

(defn index [req]
  (layout req "Productivity Self Delusion Machine"
          [:h2 "For when actual productivity is just too hard"]
          [:table
           [:tr
            [:td
             [:img {:src "/assets/images/animated-under-construction.gif"}]]
            [:td
             [:marquee "!!Under Construction!! Weren't the 90s Amazing!"]]
            [:td
             [:img {:src "/assets/images/animated-under-construction.gif"}]]]
           [:tr
            [:td]
            [:td]
            [:td "Best viewed with:" [:br] [:img {:src "/assets/images/netnow3.gif"}]]]]))

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
    (GET "/" _ index)
    (GET "/about" _ about)
    (route/not-found not-found)))

(def app (let [handler (-> handler
                           (resource/wrap-resource "public")
                           content-type/wrap-content-type)]
           (if (= "development" config/*env*)
             (reload/wrap-reload handler)
             handler)))
