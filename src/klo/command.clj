(ns klo.command
  (:refer-clojure :exclude [resolve])
  (:require [klo.config :as config]
            [klo.publish :as publish])
  (:import (com.google.cloud.tools.jib.api ImageReference)))

(defn- print-image
  [{:keys [^ImageReference image]}]
  (println (.toStringWithQualifier image)))

(defn publish
  "Simply builds and publishes images for the path passed as an argument.
   It prints the image digest after it is published."
  [opts]
  (config/with
   (-> opts
       publish/execute
       print-image)))

(defn resolve [])
