(ns borkdude.unused-deps.internal
  (:require [babashka.fs :as fs]
            [clojure.string :as str]
            [edamame.core :as e]))

#_(clojure.repl.deps/sync-deps)

(def suffix-re #"\.clj$|\.cljs$|\.cljc$")

(defn when-pred [pred x]
  (when (pred x) x))

(defn index-jar [index jar]
  (let [jar (str jar)
        jar-file (fs/file jar)]
    (if-not (fs/directory? jar-file)
      (with-open [jar-resource (java.util.jar.JarFile. jar-file)]
        (let [entries (enumeration-seq (.entries jar-resource))]
          (reduce (fn [acc e]
                    (let [raw-name (.getName e)]
                      (if (and (not (str/starts-with? raw-name "META"))
                               (re-find suffix-re raw-name))
                        (let [n (str/replace raw-name suffix-re "")
                              n (str/replace n "_" "-")
                              n (str/replace n "/" ".")
                              n (symbol n)
                              [_ group-id artifact version _]
                              (re-find #"repository/(.*)/(.*)/(.*)/.*jar" jar)]
                          (update acc n (fnil conj [])
                                  {:mvn/version version
                                   :file raw-name
                                   :group-id group-id
                                   :artifact artifact}))
                        acc)))
                  index
                  entries)))
      index)))

(defn find-dep-on-classpath [[libname {:keys [:mvn/version]}] classpath-seq]
  (let [libname+version (str (str/replace libname #"\." "/") "/" version)]
    (some (fn [part]
            (when (and (str/includes? part libname+version)
                       (str/ends-with? part ".jar"))
              part))
          classpath-seq)))

(def edamame-opts (e/normalize-opts {:allow :all
                                     :features #{:clj}
                                     :auto-resolve-ns true}))

(defn parse-ns-form [file]
  (with-open [rdr (e/reader (slurp (str file)))]
    (loop []
      (let [ns-form (e/parse-next rdr edamame-opts)]
        (if (= ::e/eof ns-form)
          nil
          (if (and (seq? ns-form)
                   (= 'ns (first ns-form)))
            (e/parse-ns-form ns-form)
            (recur)))))))

(comment
  (find-dep-on-classpath ['io.github.borkdude/lein2deps {:mvn/version "0.1.0"}] (str/split (System/getProperty "java.class.path") #":"))
  (parse-ns-form *file*)
  )
