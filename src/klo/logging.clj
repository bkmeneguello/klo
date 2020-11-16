(ns klo.logging
  (:require [clojure.tools.logging :as logging])
  (:import (ch.qos.logback.classic Level)
           (org.slf4j LoggerFactory)))

(defn setup
  [verbosity]
  (let [level (cond
                (= verbosity 0) (Level/OFF)
                (= verbosity 1) (Level/ERROR)
                (= verbosity 2) (Level/WARN)
                (= verbosity 3) (Level/INFO)
                (= verbosity 4) (Level/DEBUG)
                (= verbosity 5) (Level/TRACE)
                :else (Level/ALL))]
    (-> (LoggerFactory/getILoggerFactory)
        (.getLogger "ROOT")
        (.setLevel level))))

(defmacro trace
  [& args]
  `(logging/trace ~@args))

(defmacro debug
  [& args]
  `(logging/debug ~@args))

(defmacro info
  [& args]
  `(logging/info ~@args))

(defmacro warn
  [& args]
  `(logging/warn ~@args))

(defmacro error
  [& args]
  `(logging/error ~@args))

(defmacro fatal
  [& args]
  `(logging/fatal ~@args))

(defmacro tracef
  [& args]
  `(logging/tracef ~@args))

(defmacro debugf
  [& args]
  `(logging/debugf ~@args))

(defmacro infof
  [& args]
  `(logging/infof ~@args))

(defmacro warnf
  [& args]
  `(logging/warnf ~@args))

(defmacro errorf
  [& args]
  `(logging/errorf ~@args))

(defmacro fatalf
  [& args]
  `(logging/fatalf ~@args))
