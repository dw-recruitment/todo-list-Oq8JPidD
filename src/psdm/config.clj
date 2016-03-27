(ns psdm.config
  (:require [clojure.edn :as edn]
            [kosmos]))

;; TODO: Base the default value off of something like a
;; system property or environment variable.

(def ^{:doc     "the environment we are running in currently"
       :dynamic true}
*env* "development")

(defn load-settings []
  (->> (str "config/" *env* "/settings.edn")
       slurp
       edn/read-string))

(defn start-system []
  (->> (load-settings)
       ;; build the components
       kosmos/map->system
       ;; start the system, this will add a shutdown hook to stop
       ;; the system as well
       kosmos/start!))

(defn stop-system []
  (kosmos/stop!))

(defn not-production? []
  ;; when it comes to checking for production, it's better to check that we
  ;; are in a non-production environment. This avoids issues like "production"
  ;; being mispelled and then terrible things happening as a result.
  (#{"development" "test"} *env*))
