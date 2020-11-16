(ns klo.cli
  (:require [klo.logging :as log]
            [clojure.tools.cli :as cli]
            [clojure.string :as str]
            [clojure.stacktrace :as stacktrace]))

(defn- parse-opts
  [args option-specs]
  (let [parsed (cli/parse-opts args option-specs :in-order true :summary-fn identity)
        summary (:summary parsed)
        positional? #(every? nil? (vals (select-keys % [:short-opt :long-opt])))
        [arguments positionals] (loop [positionals (map :id (filter positional? summary))
                                       args (:arguments parsed)
                                       options {}]
                                  (let [value (first args)
                                        id (first positionals)]
                                    (if (empty? positionals)
                                      [args options]
                                      (recur (rest positionals)
                                             (rest args)
                                             (assoc options id value)))))]
    (-> (merge-with merge parsed {:options positionals})
        (assoc :arguments arguments))))

(defn- help-option-param
  [option]
  (if (every? nil? ((juxt :short-opt :long-opt) option))
    (str "<" (name (:id option)) ">")
    (format "%-3s %s"
            (if-let [opt (:short-opt option)]
              (str opt (when (:long-opt option) ","))
              "")
            (if-let [opt (:long-opt option)]
              (str opt (when-let [required (:required option)]
                         (str "=" required)))
              ""))))

(defn- generate-help
  [specs summary]
  (when-let [parent (:parent specs)]
    (print (str (:command parent) " [options] ")))
  (print (str (:command specs) " [options]"))
  (when-not (get specs :strict? true)
    (print " -- [extra arguments]"))
  (println)
  (println)
  (println (:description specs))
  (println)
  (->> summary
       (map #(format "  %-28s %s"
                     (help-option-param %)
                     (or (:desc %) "")))
       (map println-str)
       str/join))

(defn run
  [args specs]
  (let [global (parse-opts args (:spec specs))
        errors (:errors global)
        {:keys [help? command]} (:options global)
        show-help #(println (generate-help specs (:summary global)))]
    (cond
      errors (doall (map println errors))
      help? (show-help)
      command (let [name command
                    parent-specs specs
                    command-specs (get (:commands specs) name)
                    {:keys [spec command-fn] :as specs} command-specs
                    command (parse-opts (:arguments global) spec)
                    errors (:errors command)
                    {:keys [help?] :as opts} (:options command)
                    show-help #(println (generate-help (assoc specs :command name
                                                              :parent parent-specs) (:summary command)))]
                (cond
                  errors (doall (map println errors))
                  help? (show-help)
                  (nil? command-specs) (do (println (format "Unknown command \"%s\"" name))
                                           (System/exit 127))
                  :else (try
                          ((:setup-fn parent-specs) (:options global))
                          (command-fn opts :args (:arguments command))
                          (System/exit 0)
                          (catch Throwable e
                            (log/warn (ex-message e))
                            (log/debug (with-out-str (stacktrace/print-stack-trace e)))
                            (System/exit 1)))))
      :else (show-help))))
