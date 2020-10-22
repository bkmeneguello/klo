(ns klo.resolve
  (:require [clojure.java.io :as io]
            [clj-yaml.core]
            [clojure.string :as str]
            [clojure.walk :as walk]
            [klo.publish :as publish])
  (:import (java.io SequenceInputStream)
           (org.apache.commons.io FilenameUtils)))


(defn- dir-input-stream
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
  [filename]
  (if (= "-" (str/trim filename)) *in*
      (let [file (io/file filename)]
        (when (.exists file)
          (if (.isDirectory file)
            (dir-input-stream file)
            (io/reader filename))))))

(defn- make-yaml
  []
  (clj-yaml.core/make-yaml :dumper-options {:flow-style :block}))

(defn- manifest-seq
  [yaml stream]
  (->> (iterator-seq (.iterator (.loadAll yaml stream)))
       (map #(clj-yaml.core/decode % true))))

(defn- dump-manifests
  [yaml manifests]
  (->> manifests
       (map clj-yaml.core/encode)
       .iterator
       (.dumpAll yaml)))

(extend-protocol clj-yaml.core/YAMLCodec
  java.util.concurrent.Future
  (encode [data]
    (clj-yaml.core/encode @data)))

(defn- ^String repo-path
  [^String s]
  (let [re #"^klo://"]
    (when (re-find re s)
      (str/replace-first s re ""))))

(defn- ^String publish
  [opts ^String path]
  (.toStringWithQualifier (:image (publish/execute (assoc opts :path path)))))

(def ^:private cache (atom {}))

(defn- build-path
  [opts ^String path]
  (if-let [publish-job (get @cache path)]
    publish-job
    (let [publish-job (future (publish opts path))]
      (swap! cache #(assoc % path publish-job))
      publish-job)))

(defn- image-walker
  [opts value]
  (if-let [path (and (string? value) 
                     (repo-path value))]
    (build-path opts path)
    value))

(defn- publish-images
  [opts manifests]
  (walk/prewalk (partial image-walker opts) manifests))

(defn execute
  "Takes Kubernetes yaml files in the style of `kubectl apply` and
determines the set of Clojure project paths to build, containerize,
and publish."
  [{:keys [filename] :as opts}]
  (with-open [stream (input-stream filename)]
    (let [yaml (make-yaml)]
      (->> (manifest-seq yaml stream)
           (publish-images opts)
           (dump-manifests yaml)))))
