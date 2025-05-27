(ns borkdude.unused-deps 
  (:require
   [babashka.fs :as fs]
   [babashka.process :as p]
   [borkdude.unused-deps.internal :as impl]
   [clojure.edn :as edn]
   [clojure.string :as str]
   [lein2deps.api :as lein2deps]))

(defn unused-deps [opts]
  (let [root-dir (:root-dir opts)
        deps-file
        (if-let [f (:deps-file opts)]
          (if (fs/relative? f)
            (fs/file root-dir f)
            f)
          (or (impl/when-pred fs/exists? (fs/file root-dir "deps.edn"))
              (impl/when-pred fs/exists? (fs/file root-dir "project.clj"))))
        source-paths (map #(fs/file root-dir %) (:source-paths opts ["src"]))
        clojure-files (mapcat #(fs/glob % "**.{clj,cljc,cljs}") source-paths)
        requires+imports (into #{} (mapcat (fn [file]
                                             (let [{:keys [requires imports]}
                                                   (impl/parse-ns-form file)]
                                               (concat (->>
                                                       requires
                                                       (map :lib))
                                                       imports))) clojure-files))
        lein? (= "project.clj" (fs/file-name deps-file))
        deps (if lein?
               (:deps (:deps (lein2deps/lein2deps {:project-clj deps-file})))
               (:deps (edn/read-string (slurp deps-file))))
        classpath (or (:classpath opts)
                      (if lein?
                        (:out (p/sh {:dir root-dir} "lein" "classpath"))
                        (:out (p/sh {:dir root-dir} "clojure" "-Spath"))))
        classpath-seq (str/split classpath (re-pattern (System/getProperty "path.separator")))]
    {:unused-deps (vec (for [dep deps
                            :when (:mvn/version (second dep))
                            :let [jar (impl/find-dep-on-classpath dep classpath-seq)]
                            :when jar
                            :let [lib-namespaces (keys (impl/index-jar {} jar))]
                             :when (not (some #(contains? requires+imports %) lib-namespaces))]
                         dep))}))

(defn exec-fn [opts]
  (binding [*print-namespace-maps* false] (prn (unused-deps opts))))

(comment
  (unused-deps {})
  (unused-deps {:root-dir "test-resources"
                :deps-file "project.clj"
                :source-paths ["src"]})
  )
