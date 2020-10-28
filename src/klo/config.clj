(ns klo.config
  (:refer-clojure :exclude [get load])
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [klo.util :refer [deep-merge]]
            [klo.fs :as fs])
  (:import (java.nio.file Path)))

(def ^{:dynamic true :private true} *config*
  "The current configuration of the process"
  nil)

(defn- load-klo-edn
  "Loads the config from the path specified with an optional custom name."
  [^Path path & {:keys [^String filename]
                 :or {filename ".klo.edn"}}]
  (try
    (-> (.. (fs/as-path path filename) toUri)
        slurp
        edn/read-string
        (assoc :exists true
               :file (io/as-file filename)))
    (catch java.io.FileNotFoundException _ {:exists false})
    (catch Exception e {:exists true
                        :error e})))

(defn- ^Path home
  "Return the path to the user's Klo home directory."
  []
  (let [klo-home (System/getenv "KLO_HOME")
        klo-home (or (and klo-home (fs/as-path klo-home))
                     (fs/as-path (System/getProperty "user.home") ".klo"))]
    (cond-> klo-home
      (not (fs/exists? klo-home)) fs/create-dir)))

(def ^:private defaults
  {:default {:base "adoptopenjdk/openjdk8"
             :registry :registry}})

(defn load
  "Loads the configuration from a path relative to current process .klo.edn file"
  []
  (deep-merge defaults
              (load-klo-edn (home) :filename "config.edn")
              (load-klo-edn (fs/as-path ""))))

(defmacro with-config
  "Wraps the body with a loaded configuration"
  [& body]
  `(binding [*config* (load)]
     ~@body))

(defn get
  "Gets an entry from currently bound configuration merged with the config of optional path"
  [key & {:keys [path]}]
  (cond-> (clojure.core/get *config* key)
    path (deep-merge (load-klo-edn path))))
