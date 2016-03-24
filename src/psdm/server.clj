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
  (layout req "About"))

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
