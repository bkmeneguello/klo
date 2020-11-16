(ns klo.command.apply
  (:refer-clojure :exclude [apply resolve])
  (:require [klo.command.resolve :as resolve]
            [clojure.string :as str]
            [clojure.java.shell :as shell]
            [klo.logging :as log]))

(defn execute
  [opts kubectl-args]
  (let [input (resolve/execute opts)
        shell-args (vec (concat ["kubectl" "apply" "-f" "-"]
                                kubectl-args))]
    (log/debugf "Executing %s" (str/join " " shell-args))
    (-> (clojure.core/apply shell/sh (conj shell-args :in input)) :out str/trim)))
