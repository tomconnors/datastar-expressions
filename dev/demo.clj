(ns demo
  (:require
   [jsonista.core :as j]
   [starfederation.datastar.clojure.expressions :refer [->expr]]
   [boilerplate :as boiler]
   [dev.onionpancakes.chassis.core :as h]
   [starfederation.datastar.clojure.adapter.http-kit :refer [->sse-response on-open]]
   [starfederation.datastar.clojure.api :as d*]))

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
       [:p "Selected: " [:span {:data-text (->expr ($bear.result))}]])]])))

(defn home [req]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (home-page req)})

(defn routes [] [["/" {:handler home}]
                 ["/bear" {:post {:handler (fn [req]
                                             (->sse-response req
                                                             {on-open
                                                              (fn [sse-gen]
                                                                (let [v (-> req :body (j/read-value j/keyword-keys-object-mapper) :form :bear)]
                                                                  (d*/merge-signals! sse-gen
                                                                                     (j/write-value-as-string {:bear {:result v}}))))}))}}]

                 boiler/assets])

(comment
  (do
    (boiler/stop)
    (boiler/start {:port 8080 :routes-fn #'routes}))
  ;; rcf
  )
