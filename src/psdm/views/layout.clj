(ns psdm.views.layout
  (:require [hiccup.util]
            [hiccup.page :as page]
            [hiccup.core :refer :all]))

(defn include-crossorigin-css [& css-descriptors]
  (for [{:keys [href integrity crossorigin]} css-descriptors]
    [:link {:type "text/css"
            :rel "stylesheet"
            :href (hiccup.util/to-uri href)
            :integrity integrity
            :crossorigin crossorigin}]))

(defn include-crossorigin-js [& js-descriptors]
  (for [{:keys [src integrity crossorigin]} js-descriptors]
    [:script {:type "text/javascript"
              :src (hiccup.util/to-uri src)
              :integrity integrity
              :crossorigin crossorigin}]))

(defn application [page-name & contents]
  ;; Lots borrowed from: http://getbootstrap.com/getting-started/#template
  (page/html5
    {:lang "en"}
    [:head
     [:meta {:charset "utf-8"}]
     [:meta {:http-equiv "X-UA-Compatible" :content "IE=edge"}]
     [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]

     ;; bootstrap CSS
     (include-crossorigin-css
       {:href "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/css/bootstrap.min.css"
        :integrity "sha384-1q8mTJOASx8j1Au+a5WDVnPi2lkFfwwEAa8hDDdjZlpLegxhjVME1fgjWPGmkzs7"
        :crossorigin "anonymous"}
       {:href "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/css/bootstrap-theme.min.css"
        :integrity "sha384-fLW2N01lMqjakBkx3l/M9EahuwpSfeNvV63J5ezn3uZzapT0u7EYsXMjQV+0En5r"
        :crossorigin "anonymous"})

     ;; our custom styles
     (page/include-css "/style.css")

     ;; because IE...
     "
     <!-- HTML5 shim and Respond.js for IE8 support of HTML5 elements and media queries -->
     <!-- WARNING: Respond.js doesn't work if you view the page via file:// -->
     <!--[if lt IE 9]>
       <script src=\"https://oss.maxcdn.com/html5shiv/3.7.2/html5shiv.min.js\"></script>
       <script src=\"https://oss.maxcdn.com/respond/1.4.2/respond.min.js\"></script>
     <![endif]-->
     "

     [:title "Productivity Self Delusion Machine"]]
    [:body
     [:div {:class "navbar navbar-inverse navbar-fixed-top"
            :role  "navigation"}
      [:div {:class "container"}
       [:div {:class "navbar-header"}
        [:a {:class "navbar-brand" :href "/"}
         "Productivity Self Delusion Machine"]]]]

     [:div {:class "container"}
      ;[:h1 (hiccup.util/escape-html page-name)]
      contents]

     ;; jQuery JS (required by bootstrap)
     (page/include-js "https://ajax.googleapis.com/ajax/libs/jquery/1.11.3/jquery.min.js")

     ;; bootstrap JS (including here until we start using ClojureScript)
     (include-crossorigin-js
       {:src "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/js/bootstrap.min.js"
        :integrity "sha384-0mSbJDEHialfmuBBQP6A4Qrprq5OVfW37PRR3j5ELqxss1yVqOtnepnHVP9aJ7xS"
        :crossorigin "anonymous"})]))
