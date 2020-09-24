(ns klo.config
  (:refer-clojure :exclude [get load])
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [klo.util :refer [as-path]])
  (:import (java.nio.file Path)))

(def ^{:dynamic true :private true} *config* nil)

(def ^:private defaults
  {:default {:base "adoptopenjdk/openjdk8"}})

(defn- load-klo-edn
  [& {:keys [^Path path ^String filename]
      :or {path (as-path "")
           filename ".klo.edn"}}]
  (try
    (-> (.. path (resolve filename) toUri)
        slurp
        edn/read-string
        (assoc :exists true
               :file (io/as-file filename)))
    (catch java.io.FileNotFoundException _ {:exists false})
    (catch Exception e {:exists true
                        :error e})))

(defn load
  "Loads the configuration from a path relative to current process .klo.edn file"
  []
  (merge defaults (load-klo-edn)))

(defmacro with
  "Wraps the body with a loaded configuration"
  [& body]
  `(binding [*config* (load)]
     ~@body))

(defn get
  "Gets an entry from currently bound configuration"
  [& {:keys [key path]}]
  (cond-> (clojure.core/get *config* key)
    path (merge (load-klo-edn :path path))))
