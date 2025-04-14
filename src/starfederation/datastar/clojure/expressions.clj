(ns starfederation.datastar.clojure.expressions
  (:require
   [starfederation.datastar.clojure.expressions.internal :as impl]
   [backtick         :refer [template]]))

(defmacro ->expr
  "Compiles a clojure form into a datastar expression.

  TODO docs "
  [& forms]
  (let [processed-form (impl/pre-process forms)]
    #_(tap> `(template ~processed-form))
    `(impl/compile (template ~processed-form))))
