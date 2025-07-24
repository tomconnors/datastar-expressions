;; Copyright © 2025 Casey Link
;; SPDX-License-Identifier: MIT
(ns demo
  (:require
   [boilerplate :as boiler]
   [dev.onionpancakes.chassis.core :as h]
   [starfederation.datastar.clojure.adapter.http-kit :refer [->sse-response on-open]]
   [starfederation.datastar.clojure.api :as d*]
   [starfederation.datastar.clojure.expressions :refer [->expr]]))

(defn choose-button [field-name cmd value]
  [:button {:data-on-click (->expr
                            (set! ($form. ~field-name) ~value)
                            (@post ~cmd))}
   value])

(defn home-page [_]
  (h/html
   (boiler/page-scaffold
    [[:h1 "Test page"]
     [:section
      [:input {:data-bind-name ""
               :data-computed-greeting (->expr (let [name $name]
                                                 (if (str/starts-with? name "CEO")
                                                   "Greetings cult leader"
                                                   (str "Hello " (str/capitalize name) ""))))}]
      [:button {:data-on-click (->expr (alert $greeting))}
       "Greet"]]

     [:section
      [:div {:class "box box-green"
             :tabindex "0"
             :data-on-keydown (->expr (when (or (= evt.key "Enter")
                                                (&& evt.ctrlKey (= evt.key "1")))
                                        (evt.preventDefault)
                                        (alert "Key Pressed")))}
       "Select me and press Enter or Ctrl+1 to trigger an alert"]]

     [:section {:data-signals (->expr {:form {:bear nil}
                                       :bear {:result "none"}})}
      (list
       (choose-button "bear" "/bear" "Yogi")
       (choose-button "bear" "/bear" "Pooh")
       [:p "Selected: " [:span {:data-text (->expr ($bear.result))}]])]

     [:section {:data-signals (->expr {:clicked []})}
      (let [click-handler (->expr
                            ;; javascript template strings
                           #_{:clj-kondo/ignore [:not-a-function]}
                           (println ("`clicked: ${evt.srcElement.id}`"))
                            ;; array spread operator
                           (set! $clicked [...$clicked evt.srcElement.id]))]
        (list
         [:button {:id "i-am-a-button"  :data-on-click click-handler}
          "Click Me"]
         [:button {:id "but-i-am-too"  :data-on-click click-handler}
          "No, Me"]))
      [:pre {:data-text "JSON.stringify($clicked)"}]]])))

(defn home [req]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (home-page req)})

(defn routes [] [["/" {:handler home}]
                 ["/bear" {:post {:handler (fn [req]
                                             (->sse-response req
                                                             {on-open
                                                              (fn [sse-gen]
                                                                (d*/merge-signals! sse-gen
                                                                                   (boiler/->json {:bear {:result (-> req :body-params  :form :bear)}})))}))}}]

                 boiler/assets])

(comment
  (do
    (boiler/stop)
    (boiler/start {:port 8080 :routes-fn #'routes}))
  ;; rcf
  )