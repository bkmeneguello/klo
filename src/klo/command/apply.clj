(ns klo.command.apply
  (:refer-clojure :exclude [apply resolve])
  (:require [klo.command.resolve :as resolve]
            [clojure.string :as str]
            [clojure.java.shell :as shell]
            [clojure.tools.logging :as log]))

(def ^:private kubectl-keys
  #{:all
    :allow-missing-template-keys
    :cascade
    :dry-run
    :field-manager
    :force
    :force-conflicts
    :grace-period
    :openapi-patch
    :output
    :overwrite
    :prune
    :prune-whitelist
    :record
    :selector
    :server-side
    :template
    :timeout
    :validate
    :wait})

(defn execute
  [opts]
  (let [kubectl-opts (select-keys opts kubectl-keys)
        opts (dissoc opts kubectl-keys)
        input (resolve/execute opts)
        shell-args (vec (concat ["kubectl" "apply" "-f" "-"]
                                (mapcat (fn [[k v]] [(str "--" (name k) "=" v)]) kubectl-opts)))]
    (log/debugf "Executing %s" (str/join " " shell-args))
    (-> (clojure.core/apply shell/sh (conj shell-args :in input)) :out str/trim)))
