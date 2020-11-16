(ns klo.command.core
  (:refer-clojure :exclude [apply resolve])
  (:require [klo.config :as config]
            [klo.command.publish :as publish]
            [klo.command.resolve :as resolve]
            [klo.command.apply :as apply]
            [klo.util :refer [as-string]])
  (:import (com.google.cloud.tools.jib.api ImageReference)))

(defn- print-image
  [{:keys [^ImageReference target]}]
  (println (as-string target)))

(defn publish
  "Builds and publishes images for the path passed as an argument.
   It prints the image digest after it is published."
  [opts & _]
  (config/with-config
    (-> opts
        publish/execute
        print-image)))

(defn resolve
  [opts & _]
  (config/with-config
    (-> opts
        resolve/execute
        println)))

(defn apply
  [opts & {:keys [args]}]
  (config/with-config
    (-> opts
        (apply/execute args)
        println)))
