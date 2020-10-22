(ns klo.leiningen.core
  (:require [klo.util :refer [symbol->str deep-merge]]
            [klo.leiningen.uberjar :as uberjar]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [clojure.tools.logging :as log])
  (:import (java.nio.file Path Files)
           (java.io BufferedReader StringReader IOException)))

(defn- project-clj
  [^Path path]
  (.resolve path "project.clj"))

(defn project?
  [^Path path]
  (Files/isReadable (project-clj path)))

(defn- leiningen
  []
  (some #(try (-> (shell/sh % "lein")
                  :out
                  str/trim)
              (catch IOException _ nil))
        ["which" "where"]))

(defn- uberjar!
  [^Path path]
  (let [current-env (into {} (System/getenv))]
    (log/infof "Building %s" path)
    (->> (shell/sh (leiningen) "uberjar" ;;TODO: allow other tasks
                   :dir (.toFile path)
                   :env (dissoc current-env "CLASSPATH"))
         :out
         StringReader.
         BufferedReader.
         line-seq
         (filter #(re-matches #"Created .*-standalone.jar" %)) ;;FIXME: not always the same matcher
         first
         (#(str/replace % "Created " "")))))

(defn- build
  [project]
  (let [uberjar (uberjar! (:path project))]
    (assoc project :uberjar uberjar)))

(defn parse
  [^Path path]
  (let [project-clj (slurp (.toUri (project-clj path)))
        project-model (binding [*read-eval* false]
                        (read-string project-clj))
        [project-name project-version] (take 2 (drop 1 project-model))]
    (deep-merge {:build-fn build
                 :publish-fn uberjar/containerize!
                 :name (symbol->str project-name)
                 :tag project-version}
                (-> (->> (drop 3 project-model)
                         (apply hash-map))
                    (get-in [:profiles :klo] {})))))
