(ns klo.command.publish
  (:require [klo.config :as config]
            [klo.leiningen.core :as lein]
            [klo.util :refer [str->symbol as-path]]
            [clojure.java.shell :as shell]
            [clojure.tools.logging :as log]
            [clojure.string :as str])
  (:import (com.google.cloud.tools.jib.api ImageReference)
           (org.apache.commons.validator.routines UrlValidator)
           (org.apache.commons.io FilenameUtils FileUtils)
           (java.net URI)
           (java.nio.file Path Files)
           (java.nio.file.attribute FileAttribute)))

(defn- parse-uri
  "Checks if the string is a URI of one of the valid schemas"
  [^String s]
  (let [validator (UrlValidator. (into-array ["http", "https", "ssh", "git"]))]
    (when (.isValid validator s)
      (URI. s))))

(defn- parse-git-ssh-uri
  "Parse the string as a SCP-like URL then returns as an SSH URI.
   https://git-scm.com/book/en/v2/Git-on-the-Server-The-Protocols#_the_ssh_protocol"
  [^String s]
  (when-let [[_ authority path] (re-matches #"(git@[-\w.]+):(.+\.git)\/?$" s)]
    (URI. (str "ssh://" authority "/" path))))

(defn- parse-github-repo
  "Parses the string to check if it's a Github repository the returns as URI."
  ;; TODO: Check from config if HTTPS or SSH is prefered
  ;; TODO: Consider Github Enterprise?
  [^String path]
  (when (str/starts-with? path "github.com/")
    (URI. (str "https://" path (when-not (str/ends-with? path ".git") ".git")))))

(defn- create
  "Creates a new project data with default values.
   The `path` is checked as valid URL or assumed to be a local path.
   The `repo`, `name` and `tag` parameters are populated into the initial project.
   The default `base` docker image is defined into the project."
  [{:keys [^String path] :as opts}]
  (let [project (select-keys opts [:repo :name :tag])]
    ;;TODO: validate project attributes from opts
    (merge project
           (if-let [uri (some #(apply % [path])
                              [parse-uri
                               parse-git-ssh-uri
                               parse-github-repo])]
             {:uri uri}
             {:path (as-path path)}))))

(def ^:private known-archives
  ["zip" "bz2" "gz" "tar" "tgz" "tbz" "txz"])

(defn- ^boolean downloadable-artifact?
  "Checks if the URI is pointing to a know archive file"
  [^URI uri]
  (let [path (.getPath uri)]
    (FilenameUtils/isExtension path (into-array known-archives))))

(defn- ^Path download-artifact
  "Downloads and, optionally, decompress and extracts the artifact."
  ;; TODO: Implement this
  [^URI uri]
  (throw (ex-info "Download is not supported yet." {:uri uri})))

(defn- ^Path clone-repository
  "Clones a Git repository to a temporary local path then return this path"
  [^URI uri]
  (let [uri-str (str uri)
        path (str (Files/createTempDirectory "klo-" (into-array FileAttribute []))) ;;FIXME: Customize target directory
        _ (log/infof "cloning %s into %s" uri-str path)
        shellout (shell/sh "git" "clone" uri-str path)]
    (when-not (zero? (:exit shellout))
      (throw (ex-info "Failed to clone" shellout)))
    (as-path path)))

(defn- download
  "If the project is not a local path, its URL is validated and the project is 
   downloaded to a temporary location.
   If the URL is a Git remote, the project is cloned.
   If the URL is a known compressed file, it's downloaded and extracted to a 
   temporary location."
  [{:keys [^URI uri ^Path path] :as project}]
  (cond (and uri (not path))
        (merge project
               {:path (cond
                        (downloadable-artifact? uri) (download-artifact uri)
                        :else (clone-repository uri))
                :temp? true})
        :else project))

(defn- configure
  "The local path is checked for a valid Clojure project (currently only 
   Leiningen is supported). Then the project metadata is parsed to determine
   the `name` and `tag` (if not already defined).
   Also, the project specific configurations defined in `.klo.edn` are 
   overwriten using the current project name as key."
  [{:keys [^Path path] :as project}]
  (when-not (Files/isReadable path)
    (throw (ex-info "The local path is not acessible or does not exists" project)))
  (let [project (cond
                  (lein/project? path) (merge (lein/parse path) project)
                  :else (throw (ex-info "The path is not a know project" project)))
        project-config (config/get (str->symbol (:name project)) :path path)]
    (cond-> project
      (:exists project-config) (merge project-config))))

(defn- build
  "The project is built to produce a runnable standalone JAR file.
   @see https://github.com/GoogleContainerTools/jib/blob/master/docs/faq.md#i-want-to-containerize-a-jar"
  [project]
  (let [project (merge (config/get :default) project)
        build-fn (:build-fn project)]
    (build-fn project)))

(defn- publish
  "Publishes the image from the project to the repository specified."
  [{:keys [repo name tag] :as project}]
  ;; TODO: naming strategies https://github.com/google/ko/blob/3c6a907da983cda3f0c85f56ca41de16f8e20960/pkg/commands/options/publish.go#L80
  (let [image (ImageReference/of repo name tag)
        published (apply (:publish-fn project) [project image])]
    (assoc project :image published)))

(defn- cleanup
  "Check if the path is temporary then removes the directory."
  [{:keys [^Path path ^boolean temp?] :as project}]
  (when temp? ;;TODO: Check if should keep
    (log/infof "Cleaning up the directory %s" path)
    (FileUtils/deleteDirectory (.toFile path)))
  project)

(defn execute
  [opts]
  (let [project (create opts)]
    (try (-> project
             download
             configure
             build
             publish)
         (finally (cleanup project)))))
