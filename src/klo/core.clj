(ns klo.core
  (:gen-class)
  (:require [cli-matic.core :as cli]
            [klo.command.core :as cmd])
  (:import (ch.qos.logback.classic Level)
           (org.slf4j LoggerFactory)))

(def ^:private local-option
  {:as "Load into images to local docker daemon."
   :option "local"
   :type :with-flag
   :short "L"})

(def ^:private repo-option
  {:as "Docker base repository URL to wich the images will be pushed"
   :option "repo"
   :type :string
   :env "KLO_DOCKER_REPO"})

(def ^:private filename-option
  {:as "Filename, directory, or URL to files to use to create the resource"
   :option "filename"
   :type :string
   :short "f"})

(def ^:private kubectl-options
  [{:option "all"
    :as "Select all resources in the namespace of the specified resource types."
    :type :with-flag}
   {:option "allow-missing-template-keys"
    :as "If true, ignore any errors in templates when a field or map key is missing in the template. Only applies to golang and jsonpath output formats."
    :type :with-flag}
   {:option "cascade"
    :as "If true, cascade the deletion of the resources managed by this resource (e.g. Pods created by a ReplicationController).  Default true."
    :type :with-flag}
   {:option "dry-run"
    :as "Must be \"none\", \"server\", or \"client\". If client strategy, only print the object that would be sent, without sending it. If server strategy, submit server-side request without persisting the resource."
    :type :string}
   {:option "field-manager"
    :as "Name of the manager used to track field ownership."
    :type :string}
   {:option "force"
    :as "If true, immediately remove resources from API and bypass graceful deletion. Note that immediate deletion of some resources may result in inconsistency or data loss and requires confirmation."
    :type :with-flag}
   {:option "force-conflicts"
    :as "If true, server-side apply will force the changes against conflicts."
    :type :with-flag}
   {:option "grace-period"
    :as "Period of time in seconds given to the resource to terminate gracefully. Ignored if negative. Set to 1 for immediate shutdown. Can only be set to 0 when --force is true (force deletion)."
    :type :int}
   {:option "openapi-patch"
    :as "If true, use openapi to calculate diff when the openapi presents and the resource can be found in the openapi spec. Otherwise, fall back to use baked-in types."
    :type :with-flag}
   {:option "output"
    :short "o"
    :as "Output format. One of: json|yaml|name|go-template|go-template-file|template|templatefile|jsonpath|jsonpath-as-json|jsonpath-file."
    :type :string}
   {:option "overwrite"
    :as "Automatically resolve conflicts between the modified and live configuration by using values from the modified configuration"
    :type :with-flag}
   {:option "prune"
    :as "Automatically delete resource objects, including the uninitialized ones, that do not appear in the configs and are created by either apply or create --save-config. Should be used with either -l or --all."
    :type :with-flag}
   {:option "prune-whitelist"
    :as "Overwrite the default whitelist with <group/version/kind> for --prune"
    :type :string}
   {:option "record"
    :as "Record current kubectl command in the resource annotation. If set to false, do not record the command. If set to true, record the command. If not set, default to updating the existing annotation value only if one already exists."
    :type :with-flag}
   {:option "selector"
    :short "l"
    :as "Selector (label query) to filter on, supports '=', '==', and '!='.(e.g. -l key1=value1,key2=value2)"
    :type :string}
   {:option "server-side"
    :as "If true, apply runs in the server instead of the client."
    :type :with-flag}
   {:option "template"
    :as "Template string or path to template file to use when -o=go-template, -o=go-template-file. The template format is golang templates [http://golang.org/pkg/text/template/#pkg-overview]."
    :type :string}
   {:option "timeout"
    :as "The length of time to wait before giving up on a delete, zero means determine a timeout from the size of the object"
    :type :string}
   {:option "validate"
    :as "If true, use a schema to validate the input before sending it"
    :type :with-flag}
   {:option "wait"
    :as "If true, wait for resources to be gone before returning. This waits for finalizers."
    :type :with-flag}])

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
                     :opts [filename-option
                            local-option
                            repo-option]
                     :runs        cmd/resolve}
                    {:command     "apply"
                     :description "TBD."
                     :opts (concat [filename-option
                                    local-option
                                    repo-option]
                                   kubectl-options)
                     :runs        cmd/apply}]})

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
