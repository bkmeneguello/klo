(ns klo.resolve)

(defn resolve-command
  "Takes Kubernetes yaml files in the style of `kubectl apply` and
determines the set of Clojure project paths to build, containerize,
and publish."
  [{:keys [filename repo local]}] (println (str "resolve " filename " " local)))
