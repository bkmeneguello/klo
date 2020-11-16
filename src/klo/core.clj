(ns klo.core
  (:gen-class)
  (:require [klo.command.core :as cmd]
            [klo.logging :as logging]
            [klo.cli :as cli]))

(def ^:private cli-specs
  {:command     "klo"
   :description "A tool to build and deploy Clojure applications to Kubernetes"
   :version     "0.1.0"
   :spec [[nil nil "The command to invoke."
           :id :command]
          ["-v" nil "Increases the verbosity. Can be used multiple times."
           :id :verbosity
           :update-fn inc]
          [nil "--verbosity=VALUE" "Defines the verbosity, numerically or textually: 0=quiet, 1=error, 2=warn, 3=info (default), 4=debug and 5=trace."
           :id :verbosity
           :default 3
           :parse-fn (fn [value] (if (re-matches #"\d+" value)
                                   (Integer/parseInt value)
                                   (first (->> (map-indexed vector ["quiet" "error" "warn" "info" "debug" "trace"])
                                               (filter #(= value (second %)))
                                               (first)))))]
          ["-q" "--quiet" "Disable unecessary output. Usefull to be used in scripts"
           :id :verbosity
           :update-fn (constantly 0)]
          ["-h" "--help" "Display this help"
           :id :help?]]
   :setup-fn (fn [{:keys [verbosity]}] (logging/setup verbosity))
   :commands {"publish" {:spec [[nil nil "The path to the project to be published"
                                 :id :path]
                                ["-R" "--repo=REPO" "Docker base repository URL to wich the images will be pushed [$KLO_DOCKER_REPO]"]
                                ["-L" "--local" "Load into images to local docker daemon"]
                                [nil "--name=NAME" "Docker image name to replace project name"]
                                [nil "--tag=TAG" "Docker image tag to replace project version"]
                                ["-h" "--help" "Display this help"
                                 :id :help?]]
                         :command-fn cmd/publish
                         :description "This sub-command builds the provided Clojure projects into uberjars, containerizes them, and publishes them."}
              "resolve" {:spec [["-f" "--filename=PATH" "Filename, directory, or URL to files to use to create the resource"]
                                ["-R" "--repo=REPO" "Docker base repository URL to wich the images will be pushed [$KLO_DOCKER_REPO]"]
                                ["-L" "--local" "Load into images to local docker daemon"]
                                ["-h" "--help" "Display this help"
                                 :id :help?]]
                         :command-fn cmd/resolve
                         :description "This sub-command finds project path references within the provided files, builds them into uberjars, containerizes them, publishes them, and prints the resulting yaml."}
              "apply" {:spec [["-f" "--filename=PATH" "Filename, directory, or URL to files to use to create the resource"]
                              ["-R" "--repo=REPO" "Docker base repository URL to wich the images will be pushed [$KLO_DOCKER_REPO]"]
                              ["-L" "--local" "Load into images to local docker daemon"]
                              ["-h" "--help" "Display this help"
                               :id :help?]]
                       :command-fn cmd/apply
                       :description "TBD"
                       :strict? false}}})

(defn -main
  [& args]
  (cli/run args cli-specs))
