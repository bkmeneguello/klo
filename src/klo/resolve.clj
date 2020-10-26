(ns klo.resolve
  (:refer-clojure :exclude [resolve])
  (:require [clojure.java.io :as io]
            [clojure.core :as clj]
            [clj-yaml.core]
            [clojure.string :as str]
            [clojure.walk :as walk]
            [klo.publish :as publish])
  (:import (java.io SequenceInputStream)
           (org.apache.commons.io FilenameUtils)))

(defn- dir-input-stream
  "Recursively converts all YAML files from \"dir\" to inputStream then concat
   them separated by \"---\" YAML document separator."
  [dir]
  (->> (file-seq dir)
       (filter #(not (.isDirectory %))) ;;TODO: filter by name
       (filter #(FilenameUtils/isExtension (.getName %) (into-array ["yaml" "yml"])))
       (map #(partial io/input-stream %))
       (interpose #(io/input-stream (.getBytes "\n---\n")))
       (map #(apply % []))
       java.util.Vector.
       .elements
       SequenceInputStream.))

(defn- input-stream
  "Given the filename, converts it to a InputStream.
   If the filename is \"-\" reads from *in*.
   If it's a single YAML file path, reads from it.
   If it's a directory path it recursively reads all YAML files."
  [filename]
  (if (= "-" (str/trim filename)) *in*
      (let [file (io/file filename)]
        (when (.exists file)
          (if (.isDirectory file)
            (dir-input-stream file)
            (io/reader filename))))))

(defn- make-yaml
  "Creates a new instance of Yaml"
  []
  (clj-yaml.core/make-yaml :dumper-options {:flow-style :block}))

(defn- manifest-seq
  "Reads the input stream to a sequence of manifests"
  [yaml stream]
  (->> (iterator-seq (.iterator (.loadAll yaml stream)))
       (map #(clj-yaml.core/decode % true))))

(defn- dump-manifests
  "Converts the manifests to string"
  [manifests yaml]
  (->> manifests
       (map clj-yaml.core/encode)
       .iterator
       (.dumpAll yaml)))

(extend-protocol clj-yaml.core/YAMLCodec
  java.util.concurrent.Future
  (encode [data]
    (clj-yaml.core/encode @data)))

(defn- ^String repo-path
  "Check if the string is a klo URL then return its URN"
  [^String s]
  (let [re #"^klo://"]
    (when (re-find re s)
      (str/replace-first s re ""))))

(defn- ^String publish
  "Delegates to publish command and returns the published image"
  [^String path opts]
  (.toStringWithQualifier (:image (publish/execute (assoc opts :path path)))))

(def ^:private publish-cache
  "This is an atomic cache to keep publish job futures indexed by project path"
  (atom (hash-map)))

(defn- resolve
  "Resolves the path to the published image"
  [^String path opts]
  (if-let [publish-job (get @publish-cache path)]
    publish-job
    (let [publish-job (future (publish path opts))]
      (swap! publish-cache #(assoc % path publish-job))
      publish-job)))

(defn- resolver
  "If the value is a klo URL, resolves the image"
  [value opts]
  (if-let [path (and (string? value)
                     (repo-path value))]
    (resolve path opts)
    value))

(defn- publish-images
  "Search the manifest for klo URLs and replace with the image reference"
  [manifests opts]
  (walk/prewalk #(resolver % opts) manifests))

(defn execute
  "Takes Kubernetes yaml files in the style of `kubectl apply` and
   determines the set of Clojure project paths to build, containerize,
   and publish."
  [{:keys [filename] :as opts}]
  (with-open [stream (input-stream filename)]
    (let [yaml (make-yaml)]
      (-> (manifest-seq yaml stream)
          (publish-images opts)
          (dump-manifests yaml)))))
