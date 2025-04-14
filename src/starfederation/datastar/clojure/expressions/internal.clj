(ns starfederation.datastar.clojure.expressions.internal
  (:refer-clojure :exclude [compile])
  (:require
   [clojure.string :as str]
   [clojure.walk :as clojure.walk]
   [squint.compiler :as squint]))

(defn expr-println
  ([_ _ & exprs]
   (let [js (str/join "," (repeat (count exprs) "(~{})"))]
     (concat (list 'js* (str "console.log(" js ")")) exprs))))

(defn expr-do
  ([_ _] nil)
  ([_ _ & exprs]
   (let [js (str/join ", " (repeat (count exprs) "(~{})"))]
     (concat (list 'js* js) exprs))))

(defn expr-when
  [_ _ test & body]
  (list 'if test (cons 'expr/do body)))

(defn bool-expr [e]
  (vary-meta e assoc :tag 'boolean))

(defn expr-or
  ([_ _] nil)
  ([_ _ x] x)
  ([_ _ x & next]
   (let [js (str/join " || " (repeat (inc (count next)) "(~{})"))]
     (bool-expr
      (concat (list 'js* js) (cons x next))))))

(defn expr-and
  ([_ _]
   true)
  ([_ _ x]
   x)
  ([_ _ x & next]
   (let [js (str/join " && " (repeat (inc (count next)) "(~{})"))]
     (bool-expr
      (concat (list 'js* js) (cons x next))))))

(defn replace-deref
  "Post-processes compiled JavaScript to convert squint_core.deref(fn) to @fn"
  [js-string]
  (str/replace js-string
               #"squint_core\.deref\(([a-zA-Z_$][a-zA-Z0-9_$]*)\)\s*\("
               "@$1("))

(defn replace-truth
  "Post-processes compiled JavaScript to convert squint_core.truth_(x) to (!!x)"
  [js-string]
  (str/replace js-string
               #"squint_core\.truth_\(([a-zA-Z_$][a-zA-Z0-9_$]*)\)"
               "!!$1"))

(defn collect-kebab-signals
  "Collects all kebab-case signal names starting with $ from a form"
  [form]
  (into #{}
        (comp
         (filter symbol?)                                         ;; Keep symbols
         (filter #(and (clojure.string/starts-with? (name %) "$") ;; ...starting with $
                       (clojure.string/includes? (name %) "-")))  ;; ...containing dashes
         (map name))
        (tree-seq coll? seq form)))

(= #{"$record-id" "$foo-bar" "$red-panda"}
   (collect-kebab-signals
    `(do (set! ~'$record-id ~collect-kebab-signals)
         (~'@post "/defile-record" {:wut (+ $red-panda ~'$foo-bar)}))))

(defn restore-signals-casing
  "Post-processes compiled js to preserve the original casing of kebab cased signals

  Squint converts symbols like foo-bar into foo_bar, this function reverses that."
  [js-string form]
  (let [kebab-symbols (collect-kebab-signals form)
        replacements (for [sym kebab-symbols
                           :let [kebab-case sym
                                 snake-case (clojure.string/replace kebab-case #"-" "_")]]
                       [snake-case kebab-case])]

    (if (empty? replacements)
      js-string
      (reduce (fn [result [from to]]
                (clojure.string/replace result from to))
              js-string
              replacements))))

(defn js* [form]
  (->
   (squint.compiler/compile-string (str form) {:elide-imports true
                                               :elide-exports true
                                               :top-level false
                                               :context :expr
                                               :macros {'expr {'and expr-and
                                                               'or expr-or
                                                               'when expr-when
                                                               'do expr-do
                                                               'println expr-println}}})
   (replace-deref)
   (replace-truth)
   (restore-signals-casing form)
   (str/replace #"\n" " ")
   (str/trim)))

(defn compile [forms]
  (str/join "; "
            (map js* forms)))

(defn process-string-concat
  "This function converts forms whose head is a symbol starting with $ into string concatenation forms"
  [form]
  (clojure.walk/postwalk
   (fn [node]
     (if (and (sequential? node)
              (symbol? (first node))
              (.startsWith (name (first node)) "$"))
       (let [signal-ref (name (first node))
             args (rest node)
             processed-args (map (fn [arg]
                                   (if (and (sequential? arg)
                                            (= 'clojure.core/unquote (first arg)))
                                     (second arg)
                                     arg))
                                 args)]
         (list 'clojure.core/unquote
               (list 'clojure.core/symbol
                     (cons 'str (cons signal-ref processed-args)))))
       node))
   form))

;; we have a few custom macros for squint
;; they deviate from the default by producing js expressions
(def macro-replacements {'and 'expr/and
                         'or 'expr/or
                         'println 'expr/println
                         'when 'expr/when})
(defn process-macros [form]
  (clojure.walk/postwalk
   (fn [node]
     (if (and (list? node)
              (contains? macro-replacements (first node)))
       (cons (get macro-replacements (first node)) (rest node))
       node)) form))

(defn pre-process [forms]
  (-> forms
      process-string-concat
      process-macros))

(comment
  (def record {:record-id 1234})

  (compile  `((set! ~'$record-id ~(:record-id record))
              (~'@post "/defile-record")))

  (process-string-concat '(do ($wut "foo")))
  (process-string-concat '(do ($wut "foo" "bar")))
  (let [thing 1234]
    (process-string-concat `(do ($wut ~thing "bar"))))
  ;;
  )
