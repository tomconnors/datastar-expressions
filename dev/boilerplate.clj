;; Copyright © 2025 Casey Link
;; SPDX-License-Identifier: MIT
(ns boilerplate
  (:require
   [clojure.java.io :as io]
   [dev.onionpancakes.chassis.compiler :as hc]
   [dev.onionpancakes.chassis.core :as h]
   [jsonista.core :as j]
   [muuntaja.core :as m]
   [org.httpkit.server :as http-kit]
   [reitit.ring :as rr]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [ring.middleware.params :as params]
   #_[starfederation.datastar.clojure.consts :as consts]))

#_(def cdn-url
    (str "https://cdn.jsdelivr.net/gh/starfederation/datastar@"
         consts/version
         "/bundles/datastar.js"))

(defn ->json [v]
  (j/write-value-as-string v))

(defn <-json [v]
  (j/read-value v j/keyword-keys-object-mapper))

(defn page-scaffold [body]
  (hc/compile
   [[h/doctype-html5]
    [:html
     [:head
      [:meta {:charset "UTF-8"}]
      [:link {:rel :stylesheet :href "/style.css"}]
      [:script {:type :importmap}
       (h/raw
        (j/write-value-as-string {:imports
                                  {"squint-cljs/src/squint/core.js" "https://cdn.jsdelivr.net/npm/squint-cljs@0.8.145/src/squint/core.js"
                                   "squint-cljs/src/squint/string.js" "https://cdn.jsdelivr.net/npm/squint-cljs@0.8.145/src/squint/string.js"}}))]
      [:script {:type "module" :defer true}
       (h/raw
        "window.squint_core = await import('squint-cljs/src/squint/core.js');
         window.squint_string = await import('squint-cljs/src/squint/string.js');
         window.str = window.squint_string;")]
      [:script {:type "module"
                :src "/datastar.js"}]]
     [:body body]]]))

(def default-handler (rr/create-default-handler))

(def assets
  [["/datastar.js" {:get (fn [_]
                           {:status 200
                            :headers {"Content-Type" "application/javascript"}
                            :body (slurp (io/resource "datastar@patched.js"))})}]
   ["/style.css" {:get (fn [_]
                         {:status 200
                          :headers {"Content-Type" "text/css"}
                          :body (slurp (io/resource "style.css"))})}]])

(defn make-handler [routes]
  (rr/ring-handler (rr/router ["" {:middleware [params/wrap-params muuntaja/format-middleware]
                                   :muuntaja m/instance} routes]) default-handler))

(defonce !hk-server (atom nil))

(defn reboot-hk-server! [handler]
  (swap! !hk-server
         (fn [server]
           (when server
             (http-kit/server-stop! server))
           (http-kit/run-server handler
                                {:port 8080
                                 :legacy-return-value? false}))))

(defn start [{:keys [port routes-fn]
              :or {port 8080}}]
  (swap! !hk-server
         (fn [_]
           (http-kit/run-server (rr/reloading-ring-handler (fn [] (make-handler (routes-fn))))
                                {:port port
                                 :legacy-return-value? false}))))

(defn stop []
  (swap! !hk-server
         (fn [server]
           (when server
             (http-kit/server-stop! server)))))

