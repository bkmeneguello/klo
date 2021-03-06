(defproject klo "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/tools.logging "1.1.0"]
                 [org.clojure/tools.cli "1.0.194"]
                 [clj-commons/clj-yaml "0.7.0"]
                 [org.ajoberstar/ike.cljj "0.4.1"]
                 [commons-validator "1.7"]
                 [com.google.cloud.tools/jib-core "0.16.0"]
                 [ch.qos.logback/logback-classic "1.2.3"]]
  :main klo.core
  :target-path "target/%s"
  :plugins [[lein-cljfmt "0.7.0"]
            [lein-nsorg "0.3.0"]
            [io.taylorwood/lein-native-image "0.3.1"]]
  :native-image {:name "klo"
                 :jvm-opts ["-Dclojure.compiler.direct-linking=true"]
                 :opts [;"--report-unsupported-elements-at-runtime"
                        ;"--allow-incomplete-classpath"
                        "--initialize-at-build-time"
                        "--verbose"
                        "--no-server"
                        "--no-fallback"]}
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}
             :test {:dependencies [[nubank/mockfn "0.6.1"]]
                    :plugins [[lein-cloverage "1.1.2"]]}})
