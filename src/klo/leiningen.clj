(ns klo.leiningen
  (:require [klo.util :refer [symbol->str]]
            [klo.uberjar :as uberjar]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [clojure.tools.logging :as log])
  (:import (java.nio.file Path Files)
           (java.io BufferedReader StringReader)))

(defn- project-clj
  [^Path path]
  (.resolve path "project.clj"))

(defn project?
  [^Path path]
  (Files/isReadable (project-clj path)))

(defn- uberjar!
  [^Path path]
  (let [current-env (into {} (System/getenv))]
    (log/infof "Building %s" path)
    (->> (shell/sh "lein" "uberjar" ;;TODO: allow other tasks
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
    {:build build
     :publish uberjar/containerize!
     :name (symbol->str project-name)
     :tag project-version}))
