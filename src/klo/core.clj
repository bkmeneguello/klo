(ns klo.core
  (:gen-class)
  (:require [cli-matic.core :as cli]
            [klo.command.core :as cmd])
  (:import (ch.qos.logback.classic Level)
           (org.slf4j LoggerFactory)))

(def ^:private local-option
  {:as "Load into images to local docker daemon."
   :option "local?"
   :type :with-flag
   :short "L"})

(def ^:private repo-option
  {:as "Docker base repository URL to wich the images will be pushed"
   :option "repo"
   :type :string
   :short "R"
   :env "KLO_DOCKER_REPO"})

(def ^:private CONFIGURATION
  {:command     "klo"
   :description "A tool to build and deploy Clojure applications to Kubernetes"
   :version     "0.1.0"
   :opts        [{:as      "Log verbosity, the higher the number more information is printed"
                  :default 3
                  :option  "verbosity"
                  :short   "v"
                  :type    :int}
                 {:as      "Suppress any progress output. Best suited for scripts"
                  :default false
                  :option  "quiet"
                  :short   "q"
                  :type    :with-flag}]
   :subcommands    [{:command     "publish"
                     :description "This sub-command builds the provided Clojure projects into uberjars, containerizes them, and publishes them."
                     :opts [{:as "Project Path"
                             :option "path"
                             :type :string
                             :default "."
                             :short 0}
                            repo-option
                            {:as "Docker image name to replace project name"
                             :option "name"
                             :type :string}
                            {:as "Docker image tag to replace project version"
                             :option "tag"
                             :type :string}
                            local-option]
                     :runs        cmd/publish}
                    {:command     "resolve"
                     :description "This sub-command finds project path references within the provided files, builds them into uberjars, containerizes them, publishes them, and prints the resulting yaml."
                     :opts [{:as "Filename, directory, or URL to files to use to create the resource"
                             :option "filename"
                             :type :string
                             :short "f"}
                            local-option
                            repo-option]
                     :runs        cmd/resolve}]})

(defn- setup-logging
  "Check command line arguments for verbosity value and setup ROOT logger"
  [args]
  (let [commandline (:commandline (cli/parse-command-line args CONFIGURATION))
        quiet (:quiet commandline)
        verbosity (:verbosity commandline)
        level (cond
                (or quiet (= verbosity 0)) (Level/OFF)
                (= verbosity 1) (Level/ERROR)
                (= verbosity 2) (Level/WARN)
                (= verbosity 3) (Level/INFO)
                (= verbosity 4) (Level/DEBUG)
                (= verbosity 5) (Level/TRACE)
                :else (Level/ALL))]
    (-> (LoggerFactory/getILoggerFactory)
        (.getLogger "ROOT")
        (.setLevel level))))

(defn -main
  [& args]
  (setup-logging args)
  (cli/run-cmd args CONFIGURATION))
