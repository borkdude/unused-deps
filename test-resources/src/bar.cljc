(ns bar
  (:require #?(:clj [clojure.edn :as edn]
               :cljs [clojure.reader :as edn])))

(edn/read-string "{}")
