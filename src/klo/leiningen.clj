(ns klo.leiningen
  (:require [klo.util :refer [as-string deep-merge]]
            [klo.uberjar :as uberjar]
            [ike.cljj.file :as fs]
            [clojure.java.shell :as shell]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.tools.logging :as log])
  (:import (clojure.lang Keyword)
           (java.nio.file Path)
           (java.io IOException)
           (java.util Map)))

(defn- ^String leiningen
  "Finds the leiningen command in the system path"
  []
  (some #(try (-> (shell/sh % "lein") :out str/trim)
              (catch IOException _ nil))
        ["which" "where"]))

(defn- uberjar
  "Invokes the leiningen uberjar target"
  [^Path path]
  (log/infof "Building %s" path)
  (let [current-env (into {} (System/getenv))
        out (-> (shell/sh (leiningen) "uberjar" ;;TODO: allow other tasks
                          :dir (.toFile path)
                          :env (dissoc current-env "CLASSPATH")) :out)
        reader (io/reader (.getBytes out))
        match (->> (line-seq reader)
                   (filter #(re-matches #"Created .*-standalone.jar" %)) ;;FIXME: not always the same matcher
                   first)]
    (str/replace match "Created " "")))

(defn- ^Map build
  "Build the project uberjar"
  [^Map project]
  (let [uberjar (uberjar (:path project))]
    (assoc project :uberjar uberjar)))

(defn- ^Path project-clj
  [^Path path]
  (.resolve path "project.clj"))

(defn ^boolean project?
  "Checks if the project is a Leinigen project"
  [{:keys [^Path path ^Keyword builder]}]
  (if (some? builder)
    (= :leiningen builder)
    (fs/exists? (project-clj path))))

(defn ^Map parse
  "Parses the path as a Leiningen project, gathering some information from the project.clj"
  [^Path path]
  (let [project-clj (slurp (.toUri (project-clj path)))
        project-model (binding [*read-eval* false]
                        (read-string project-clj))
        [model-head model-data] (split-at 3 project-model)
        [project-name project-version] (drop 1 model-head)
        data (apply hash-map model-data)]
    (deep-merge {:builder :leiningen
                 :build-fn build
                 :publish-fn uberjar/containerize
                 :name (as-string project-name)
                 :tag project-version}
                (get-in data [:profiles :klo] {}))))
