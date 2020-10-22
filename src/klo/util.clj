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
  [path & more]
  (Paths/get path (into-array String more)))

(defn deep-merge
  [a & maps]
  (if (map? a)
    (apply merge-with deep-merge a maps)
    (apply merge-with deep-merge maps)))
