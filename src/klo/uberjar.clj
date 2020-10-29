(ns klo.uberjar
  (:require [klo.util :refer [->image]]
            [ike.cljj.file :as fs]
            [clojure.tools.logging :as log])
  (:import (com.google.cloud.tools.jib.api Containerizer ImageReference Jib RegistryImage DockerDaemonImage)
           (com.google.cloud.tools.jib.api.buildplan AbsoluteUnixPath FileEntriesLayer)
           (com.google.cloud.tools.jib.event.events ProgressEvent)
           (com.google.cloud.tools.jib.frontend CredentialRetrieverFactory)))

(defn- str->unix-path
  [^String filename]
  (AbsoluteUnixPath/get filename))

(defn- ->progress-event-consumer
  []
  (reify java.util.function.Consumer
    (accept [this progressEvent]
      (log/debug (.. progressEvent getAllocation getDescription)))))

(defn- ->log-event-consumer
  []
  (reify java.util.function.Consumer
    (accept [this logEvent]
      (log/debug (. logEvent getMessage)))))

(defn- ->credential-retriever
  [^ImageReference image]
  (-> (CredentialRetrieverFactory/forImage image (->log-event-consumer))
      .dockerConfig))

(defn- ->registry
  [registry ^ImageReference image]
  (condp = registry
    :docker-daemon (DockerDaemonImage/named image)
    (-> (RegistryImage/named image)
        (.addCredentialRetriever (->credential-retriever image)))))

(defn containerize
  [{:keys [name base ^ImageReference target uberjar registry]}]
  (log/infof "Using base %s for %s" base name)
  (log/infof "Building and publishing %s" (.toStringWithQualifier target))
  (let [entrypoint-jar "/opt/app.jar" ;;TODO: Make configurable
        container (-> (Jib/from base)
                      (.addFileEntriesLayer (-> (FileEntriesLayer/builder)
                                                (.setName name)
                                                (.addEntry (fs/as-path uberjar) (str->unix-path entrypoint-jar))
                                                .build))
                      (.setEntrypoint (into-array ["java" "-jar" entrypoint-jar])) ;;TODO: Make configurable
                      (.containerize (-> (Containerizer/to (->registry registry target))
                                         (.addEventHandler ProgressEvent (->progress-event-consumer)))))
        published-image (->image (format "%s@%s" (.getTargetImage container) (.getDigest container)))]
    (log/infof "Published %s" published-image)
    published-image))
