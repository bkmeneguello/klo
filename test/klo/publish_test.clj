(ns klo.publish-test

  (:require [clojure.test :refer [deftest testing is are]]
            [klo.command.publish]
            [klo.config]
            [klo.fs :as fs]
            [klo.util :refer [->image sha256]]
            [klo.leiningen.core :as lein]
            [clojure.java.shell :as shell]
            [mockfn.macros :refer [providing verifying]]
            [mockfn.matchers :as matcher])
  (:import (java.net URI)))

(deftest test-as-uri
  (let [as-uri #'klo.command.publish/as-uri]
    (testing "invalid URI"
      (are [path] (nil? (as-uri path))
        nil
        ""
        "repo"
        "path/to/repo"
        "./path/to/repo"
        "../path/to/repo"
        "/path/to/repo"
        "path\\to\\repo"
        "C:\\path\\to\\repo")
      (is (nil? (as-uri "file://path/to/repo")) "File URIs cannot be relative"))
    (testing "valid URI"
      (are [uri path] (= uri (as-uri path))
        (URI. "file:///path/to/repo") "file:///path/to/repo"
        (URI. "http://example.com") "http://example.com"
        (URI. "https://example.com") "https://example.com"
        (URI. "https://github.com/user/repo.git") "github.com/user/repo"
        (URI. "https://github.com/user/repo.git") "github.com/user/repo.git" ;;FIXME: Does this makes sense?
        (URI. "ssh://git@example.com/user/repo.git") "git@example.com:user/repo.git"
        (URI. "ssh://git@example.com/user/repo.git") "ssh://git@example.com/user/repo.git"))))

(deftest test-create
  (let [create #'klo.command.publish/create]
    (testing "create"
      (testing "with empty path"
        (is (thrown-with-msg? Exception #"Empty paths are invalid" (create {}))
            "What are you trying to create from an empty path?"))
      (testing "with local project path"
        (is (= {:path (fs/as-path "path/to/repo")}
               (create {:path "path/to/repo"})))
        (is (= {:path (fs/as-path "/path/to/repo")}
               (create {:path "file:///path/to/repo"}))))
      (testing "with remote project path"
        (is (= {:uri (URI. "https://github.com/user/repo.git")}
               (create {:path "github.com/user/repo"}))))
      (testing "build to local repository"
        (is (= {:path (fs/as-path "path/to/repo") :repo "klo.local" :registry :docker-daemon}
               (create {:path "path/to/repo" :local? true})))
        (testing "should preserve user-defined repository"
          (is (= {:path (fs/as-path "path/to/repo") :repo "user.repo" :registry :docker-daemon}
                 (create {:path "path/to/repo" :repo "user.repo" :local? true}))))))))

(deftest test-download
  (let [download #'klo.command.publish/download]
    (testing "download"
      (testing "an artifact"
        (is (thrown-with-msg? Exception #"Download is not supported yet"
                              (download {:uri (URI. "https://example.com/repo.zip")}))
            "Not today, maybe in a future release"))
      (testing "a local project"
        (is (= {:path (fs/as-path "/path/to/repo")}
               (download {:path (fs/as-path "/path/to/repo")}))))
      (testing "a Git clone"
        (let [uri (URI. "https://github.com/user/repo.git")
              path (fs/as-path "/tmp/klo-fake-repo")]
          (is (= {:uri uri
                  :path path
                  :temp? true}
                 (providing [(shell/sh "git" "clone" "https://github.com/user/repo.git" (matcher/a String)) {:exit 0}
                             (#'klo.command.publish/create-temp-dir-path) (fs/as-path "/tmp/klo-fake-repo")]
                            (download {:uri uri}))))
          (is (thrown-with-msg? Exception #"Failed to clone"
                                (providing [(shell/sh "git" "clone" "https://github.com/user/repo.git" (matcher/a String)) {:exit 1}]
                                           (download {:uri uri})))
              "An error is thrown when the Git clone fails")))
      (testing "an already downloaded project"
        (let [uri (URI. "https://github.com/user/repo.git")
              path (fs/as-path "/tmp/klo-fake-repo")]
          (is (= {:uri uri :path path}
                 (providing [(shell/sh "git" "clone" "https://github.com/user/repo.git" (matcher/a String)) {:exit 0}]
                            (download {:uri uri
                                       :path path})))))))))

(deftest test-configure
  (let [configure #'klo.command.publish/configure]
    (testing "configure"
      (testing "a non-existant path"
        (is (thrown-with-msg? Exception #"The local path is not acessible or does not exists"
                              (let [path (fs/as-path "/tmp/klo-fake-repo")]
                                (providing [(fs/exists? path) false]
                                           (configure {:path path}))))))
      (testing "an unknown project"
        (is (thrown-with-msg? Exception #"The path is not a know project"
                              (let [path (fs/as-path "/tmp/klo-fake-repo")]
                                (providing [(fs/exists? path) true
                                            (lein/project? path) false]
                                           (configure {:path path}))))))
      (testing "a simple Leiningen project"
        (let [path (fs/as-path "/tmp/klo-fake-repo")]
          (is (= {:path path}
                 (providing [(fs/exists? path) true
                             (lein/project? path) true
                             (lein/parse path) {}]
                            (configure {:path path})))))
        (testing "with project config"
          (let [path (fs/as-path "/tmp/klo-fake-repo")]
            (is (= {:path path :exists true} ;;FIXME: "exists" is meaningless here
                   (providing [(fs/exists? path) true
                               (#'klo.config/load-klo-edn path) {:exists true}
                               (lein/project? path) true
                               (lein/parse path) {}]
                              (configure {:path path}))))))))))

(deftest test-build
  (let [build #'klo.command.publish/build]
    (testing "build"
      (let [build-fn identity]
        (is (= {:build-fn build-fn}
               (build {:build-fn build-fn})))))))

(deftest test-publish
  (let [publish #'klo.command.publish/publish]
    (testing "publish"
      (let [target (->image nil "test" (sha256 ""))
            image (->image nil "test" nil)
            publish-fn (fn [_] image)]
        (is (= {:publish-fn publish-fn :name "test" :target target :image image}
               (publish {:publish-fn publish-fn :name "test" :target target})))))))

(deftest test-cleanup
  (let [cleanup #'klo.command.publish/cleanup]
    (testing "cleanup"
      (let [path (fs/as-path "/tmp/klo-fake-repo")]
        (testing "a local path"
          (is (= {:path path :temp? false}
                 (verifying [(fs/delete-dir path) nil (matcher/exactly 0)]
                            (cleanup {:path path :temp? false})))))
        (testing "a temp path"
          (is (= {}
                 (verifying [(fs/delete-dir path) nil (matcher/at-least 1)]
                            (cleanup {:path path :temp? true})))))))))
