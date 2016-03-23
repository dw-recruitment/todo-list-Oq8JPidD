(ns psdm.server)

(defn app [request]
  {:status 200
   :headers {"Content-Type" "text/plain"}
   :body "You did it!"})
