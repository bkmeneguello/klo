(ns klo.util
  (:require [clojure.string :as str])
  (:import (clojure.lang Symbol)
           (com.google.cloud.tools.jib.api ImageReference)
           (java.security MessageDigest)))

(defprotocol SymbolCoercions
  (^Symbol as-symbol [x] "Coerce argument to a symbol."))
(defprotocol StringCoercions
  (^String as-string [x] "Coerce argument to a string."))

(extend-protocol SymbolCoercions
  nil
  (as-symbol [_] nil)
  String
  (as-symbol [s] (symbol s))
  Symbol
  (as-symbol [s] s))

(extend-protocol StringCoercions
  nil
  (as-string [_] nil)
  String
  (as-string [s] s)
  Symbol
  (as-string [s] (str/join "/" (remove nil? ((juxt namespace name) s))))
  ImageReference
  (as-string [i] (.toStringWithQualifier i)))

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

(defn sha256
  "from https://gist.github.com/jizhang/4325757#gistcomment-2196746"
  [^String s]
  (let [algorithm (MessageDigest/getInstance "SHA-256")
        raw (.digest algorithm (.getBytes s))]
    (format "%064x" (BigInteger. 1 raw))))
