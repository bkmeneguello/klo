(ns klo.config
  (:refer-clojure :exclude [get load])
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [klo.util :refer [as-path deep-merge]])
  (:import (java.nio.file Path Files)
           (java.nio.file.attribute FileAttribute)))

(def ^{:dynamic true :private true} *config* nil)

(def ^:private defaults
  {:default {:base "adoptopenjdk/openjdk8"}})

(defn- load-klo-edn
  [^Path path & {:keys [^String filename]
                 :or {filename ".klo.edn"}}]
  (try
    (-> (.. path (resolve filename) toUri)
        slurp
        edn/read-string
        (assoc :exists true
               :file (io/as-file filename)))
    (catch java.io.FileNotFoundException _ {:exists false})
    (catch Exception e {:exists true
                        :error e})))

(defn- home
  "Return full path to the user's Klo home directory."
  []
  (let [klo-home (System/getenv "KLO_HOME")
        klo-home (or (and klo-home (as-path klo-home))
                     (as-path (System/getProperty "user.home") ".klo"))]
    (cond-> klo-home
      (not (Files/isReadable klo-home)) (Files/createDirectory (into-array FileAttribute [])))))

(defn load
  "Loads the configuration from a path relative to current process .klo.edn file"
  []
  (deep-merge defaults
              (load-klo-edn (home) :filename "config.edn")
              (load-klo-edn (as-path ""))))

(defmacro with
  "Wraps the body with a loaded configuration"
  [& body]
  `(binding [*config* (load)]
     ~@body))

(defn get
  "Gets an entry from currently bound configuration"
  [& {:keys [key path]}]
  (cond-> (clojure.core/get *config* key)
    path (deep-merge (load-klo-edn path))))
