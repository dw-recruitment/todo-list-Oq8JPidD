(ns psdm.server
  (:require [hiccup.core :refer :all]
            [psdm.config :as config]
            [ring.middleware.content-type :as content-type]
            [ring.middleware.resource :as resource]
            [ring.middleware.reload :as reload]))

(defn handler [_]
  {:status  200
   :headers {"Content-Type" "text/html"}
   :body    (html [:head [:title "Productivity Self Delusion Machine"]]
                  [:body
                   [:h1 "Productivity Self Delusion Machine"]
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
                     [:td "Best viewed with:" [:br] [:img {:src "/assets/images/netnow3.gif"}]]]]])})

(def app (let [handler (-> handler
                           (resource/wrap-resource "public")
                           content-type/wrap-content-type)]
           (if (= "development" config/*env*)
             (reload/wrap-reload handler)
             handler)))
