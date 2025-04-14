(ns user
  (:require [starfederation.datastar.clojure.expressions :refer [->expr]]))

;; Samples

(def record {:record-id "1234"})

;; You have to unquote (~) forms you want evaluated
;; Otherwise no quoting is needed!
;; vars and locals are available for evaluation
(let [val 42]
  (->expr
   (set! $forty-two ~val)))
;; => "$forty-two = 42;"

(let [val (random-uuid)]
  (->expr
   (set! $forty-two ~(str val))))
;; => "$forty-two = \"745a9225-890f-41a7-9fc4-008770a68e7e\";"

;; kebab case preservation
(->expr
 (set! $record-id ~(:record-id record)))
;; => "$record-id = \"1234\";"

;; actually... all case preservation :)
(->expr
 (set! $record_id ~(:record-id record)))
;; => "$record_id = \"1234\";"

(->expr
 (set! $recordId ~(:record-id record)))
;; => "$recordId = \"1234\";"

;; namespaced signals work of course
(->expr
 (set! $person.first-name "alice"))
;; => "$person.first-name = \"alice\";"

;; primitive functions work too (squint adds parens, but its ok)
(let [val 1]
  (->expr
   (set! $forty-two (+ ~val $forty-one))))
;; => "$forty-two = (1) + ($forty-one);"

;; calling js functions:
(->expr (pokeBear $bear-id))
;; => "pokeBear($bear-id)"

;; expr with multiple statements are in order like you would expect
(->expr
 (set! $bear-id 1234)
 (pokeBear $bear-id)
 (@post "/bear-poked"))
;; => "$bear-id = 1234;; pokeBear($bear-id); @post(\"/bear-poked\")"

;; You can build dynamic signal names by using the $signal in the first position
(let [field-name "name"]
  (->expr
   (set! ($bear. ~field-name) "Yogi")
   (@post "/bear")))
;; => "$bear.name = \"Yogi\";; @post(\"/bear\")"

;; logical conjunctions and disjunctions
(->expr (and (= $my-signal "bar")
             "ret-val"))
;; => "(($my-signal) === (\"bar\")) && (\"ret-val\")"

;; But you should probably use when/if
(->expr (when (= $my-signal "bar")
          "ret-val"))
;; => "((($my-signal) === (\"bar\")) ? ((\"ret-val\")) : (null))"
(->expr (if (= $my-signal "bar")
          "true-val"
          "false-val"))
;; => "((($my-signal) === (\"bar\")) ? (\"true-val\") : (\"false-val\"))"

;; A few other variations
(->expr (&& (or (= evt.key "Enter")
                (&& evt.ctrlKey (= evt.key "1")))
            (alert "Key Pressed")))
;; => "(((evt.key) === (\"Enter\")) || ((evt.ctrlKey) && ((evt.key) === (\"1\")))) && (alert(\"Key Pressed\"))"

;; This one is interesting, see how it uses the , operator to separate sub-expressions
(->expr (when  (= evt.key "Enter")
          (evt.preventDefault)
          (alert "Key Pressed")))
;; => "(((evt.key) === (\"Enter\")) ? ((evt.preventDefault()), (alert(\"Key Pressed\"))) : (null))"

;; And here is one for data-class
(->expr {"hidden" (&& $fetching-bears
                      (= $bear-id 1))})
;; => "({ \"hidden\": ($fetching-bears) && (($bear-id) === (1)) })"

;; It also does edn->json conversion, so setting initial signals is possible
(->expr {:my-signal "init-value"})
;; => "({ \"my-signal\": \"init-value\" })"

;; A let block is transpiled to a IIFE, this requires a patched version of datastar
(->expr
 (let [value $my-signal]
   (println value)
   (and (= $my-signal "bear")
        (@post "/foo"))))
;; => "(() => { const value1 = $my-signal; console.log((value1)); return (($my-signal) === (\"bear\")) && (@post(\"/foo\")); })()"
