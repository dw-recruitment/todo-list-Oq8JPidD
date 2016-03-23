(ns psdm.config)

;; TODO: Base the default value off of something like a
;; system property or environment variable.

(def ^{:doc     "the environment we are running in currently"
       :dynamic true}
*env* "development")
