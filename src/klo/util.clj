(ns klo.util
  (:require [clojure.string :as str])
  (:import (java.nio.file Paths)))

(defn symbol->str
  [s]
  (str/join "/" ((juxt namespace name) s)))

(defn str->symbol
  [s]
  (symbol s))

(defn as-path
  [filename]
  (Paths/get filename (into-array String [])))
