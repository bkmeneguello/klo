(ns klo.fs
  (:import (java.net URI)
           (java.nio.file Paths Path Files)
           (java.nio.file.attribute FileAttribute)
           (org.apache.commons.io FileUtils)))

(defprotocol Coercions
  (^Path as-path [x] [x other] "Coerce argument to a path."))

(extend-protocol Coercions
  nil
  (as-path
    ([_] nil)
    ([_ _] nil))
  String
  (as-path
    ([s] (Paths/get s (into-array String [])))
    ([s other] (Paths/get s (into-array String [other]))))
  Path
  (as-path
    ([p] p)
    ([p other] (.resolve p other)))
  URI
  (as-path
    ([u] (Paths/get u))
    ([u other] (as-path (as-path u) other))))

(defn ^boolean path?
  ;;TODO: Convert to protocol?
  [^URI uri]
  (= "file" (.getScheme uri)))

(defn ^boolean exists?
  [^Path path]
  (Files/isReadable path))

(defn ^Path create-dir
  [^Path path]
  (Files/createDirectory path (into-array FileAttribute [])))

(defn delete-dir
  [^Path path]
  (FileUtils/deleteDirectory (.toFile path)))
