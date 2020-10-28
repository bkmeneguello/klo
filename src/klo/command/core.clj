(ns klo.command.core
  (:refer-clojure :exclude [resolve])
  (:require [klo.config :as config]
            [klo.command.publish :as publish]
            [klo.command.resolve :as resolve]
            [klo.util :refer [as-string]])
  (:import (com.google.cloud.tools.jib.api ImageReference)))

(defn- print-image
  [{:keys [^ImageReference target]}]
  (println (as-string target)))

(defn publish
  "Simply builds and publishes images for the path passed as an argument.
   It prints the image digest after it is published."
  [opts]
  (config/with-config
    (-> opts
        publish/execute
        print-image)))

(defn resolve
  [opts]
  (config/with-config
    (-> opts
        resolve/execute
        println)))
