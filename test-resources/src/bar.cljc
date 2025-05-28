(ns bar
  (:require #?(:clj [clojure.edn :as edn]
               :cljs [clojure.reader :as edn]
               foobar))
  (:import fooBar))

(edn/read-string "{}")
