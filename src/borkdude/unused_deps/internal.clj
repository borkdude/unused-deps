(ns borkdude.unused-deps.internal
  (:require [babashka.fs :as fs]
            [clojure.string :as str]
            [edamame.core :as e]))

#_(clojure.repl.deps/sync-deps)

(def suffix-re #"\.clj$|\.cljs$|\.cljc$|\.class")

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

;; copied form edamame to include support for :import

(defn- libspec?
  "Returns true if x is a libspec"
  [x]
  (or (symbol? x)
      (and (vector? x)
           (or
            (nil? (second x))
            (keyword? (second x))))))

(defn- prependss
  "Prepends a symbol or a seq to coll"
  [x coll]
  (if (symbol? x)
    (cons x coll)
    (concat x coll)))

(defn- load-lib
  [prefix lib & options]
  (let [lib (if prefix (symbol (str prefix \. lib)) lib)
        opts (apply hash-map options)]
    (assoc opts :lib lib)))

(defn- load-libs
  [kw args]
  (let [args* (cons kw args)
        flags (filter keyword? args*)
        opts (interleave flags (repeat true))
        args* (filter (complement keyword?) args*)]
    (mapcat (fn [arg]
              (if (libspec? arg)
                [(apply load-lib nil (prependss arg opts))]
                (let [[prefix & args*] arg]
                  (when (nil? prefix)
                    (throw (ex-info "prefix cannot be nil"
                                    {:args args})))
                  (mapcat (fn [arg]
                            [(apply load-lib prefix (prependss arg opts))])
                          args*))))
            args*)))

(defn- -ns
  [[_ns name & references]]
  (let [docstring  (when (string? (first references)) (first references))
        references (if docstring (next references) references)
        name (if docstring
               (vary-meta name assoc :doc docstring)
               name)
        metadata   (when (map? (first references)) (first references))
        references (if metadata (next references) references)
        references (filter seq? references)
        references (group-by first references)
        requires (mapcat #(load-libs :require (rest %)) (:require references))
        imports (mapcat (fn [[_ & specs]]
                          (mapcat (fn [spec]
                                    (if (symbol? spec) spec
                                        (let [[pkg & classes] spec]
                                          (map #(symbol (str pkg "." %)) classes))))
                                  specs)) (:import references))]
    ;;(println exp)
    {:current name
     :meta metadata
     :requires requires
     :aliases (reduce (fn [acc require]
                        (if-let [alias (or (:as require)
                                           (:as-alias require))]
                          (assoc acc alias (:lib require))
                          acc))
                      {}
                      requires)
     :imports imports}))

(defn parse-ns-form* [ns-form]
  (-ns ns-form))

;;;; Scratch

(comment
  (parse-ns-form* '(ns foo (:require [foo :as dude])))
  (parse-ns-form* '(ns foo (:import [foo Bar Baz])))
  (parse-ns-form* '(ns clj-kondo.impl.analysis.java
                     {:no-doc true}
                     (:require
                      [clj-kondo.impl.utils :as utils]
                      [clojure.java.io :as io]
                      [clojure.set :as set]
                      [clojure.string :as str])
                     (:import
                      [com.github.javaparser JavaParser Range]
                      [com.github.javaparser.ast
                       CompilationUnit
                       Modifier
                       Modifier$Keyword
                       Node] )))
  )



(def edamame-opts (e/normalize-opts {:all true
                                     :features #{:clj}
                                     :read-cond :allow
                                     :auto-resolve-ns true}))

(defn parse-ns-form [file]
  (with-open [rdr (e/reader (slurp (str file)))]
    (loop []
      (let [ns-form (e/parse-next rdr edamame-opts)]
        (if (= ::e/eof ns-form)
          nil
          (if (and (seq? ns-form)
                   (= 'ns (first ns-form)))
            (parse-ns-form* ns-form)
            (recur)))))))



(comment
  (find-dep-on-classpath ['io.github.borkdude/lein2deps {:mvn/version "0.1.0"}] (str/split (System/getProperty "java.class.path") #":"))
  (parse-ns-form *file*)
  )
