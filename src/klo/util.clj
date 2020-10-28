(ns klo.util
  (:require [clojure.string :as str])
  (:import (clojure.lang Symbol)
           (com.google.cloud.tools.jib.api ImageReference)))

(defprotocol Coercions
  (^Symbol as-symbol [x] "Coerce argument to a symbol.")
  (^String as-string [x] "Coerce argument to a string."))

(extend-protocol Coercions
  nil
  (as-symbol [_] nil)
  (as-string [_] nil)
  String
  (as-symbol [s] (symbol s))
  (as-string [s] s)
  Symbol
  (as-symbol [s] s)
  (as-string [s] (str/join "/" ((juxt namespace name) s))))

(defn deep-merge
  [a & maps]
  (if (map? a)
    (apply merge-with deep-merge a maps)
    (apply merge-with deep-merge maps)))

(defn ->image
  ([image]
   (ImageReference/parse image))
  ([repo name qualifier]
   (ImageReference/of repo name qualifier))
  ([repo name tag digest]
   (ImageReference/of repo name tag digest)))
