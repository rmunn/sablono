(ns sablono.benchmark
  (:require-macros [cemerick.cljs.test :refer [is deftest testing]]
                   [sablono.core :refer [html]])
  (:require [cemerick.cljs.test :as t]
            [crate.core :as crate]
            [goog.dom :as gdom]
            [reagent.core :as reagent]
            [sablono.test :refer [body]]))

(defn reagent-template [datum]
  [:li [:a {:href (str "#show/" (:key datum))}]
   [:div {:id (str "item" (:key datum))
          :className ["class1" "class2"]}
    [:span {:className "anchor"} (:name datum)]]])

(defn crate-template [datum]
  (crate/html
   [:li [:a {:href (str "#show/" (:key datum))}]
    [:div {:id (str "item" (:key datum))
           :class ["class1" "class2"]}
     [:span {:class "anchor"} (:name datum)]]]))

(defn jquery-template [datum]
  (-> "<li>" js/jQuery
      (.append
       (-> "<a>" js/jQuery
           (.attr "href" (str "#show/" (:key datum)))
           (.addClass "anchor")
           (.append (-> "<div>" js/jQuery
                        (.addClass "class1")
                        (.addClass "class2")
                        (.attr "id" (str "item" (:key datum)))
                        (.append (-> "<span>" js/jQuery (.text (:name datum))))))))))

(defn react-template [datum]
  (js/React.DOM.li
   (js/React.DOM.a #js {:href (str "#show/" (:key datum))})
   (js/React.DOM.div
    #js {:id (str "item" (:key datum))
         :className "class1 class2"}
    (js/React.DOM.span #js {:className "anchor"} (:name datum)))))

(defn sablono-template [datum]
  (html [:li [:a {:href (str "#show/" (:key datum))}]
         [:div {:id (str "item" (:key datum))
                :class ["class1" "class2"]}
          [:span {:class "anchor"} (:name datum)]]]))

(defn run-test [root data li-fn render-fn]
  (let [now (js/Date.)]
    (render-fn root (map li-fn data))
    (/ (- (.getTime (js/Date.))
          (.getTime now)) 1000)))

(defn gen-data []
  (for [i (range 1e4)]
    {:key (rand-int 1e6)
     :name (str "product" i)}))

(defn render-append [root children]
  (let [ul (goog.dom/createDom "ul")]
    (doseq [child children]
      (goog.dom/append ul child))
    (goog.dom/append root ul)))

(defn render-reagent [root children]
  (reagent/render-component [:ul children] root))

(defn render-react [root children]
  (let [render-fn #(this-as this (html [:ul children]))
        component (js/React.createClass #js {:render render-fn})]
    (js/React.renderComponent (component) root)))

(defn time-test [data]
  (for [[key li-fn render-fn]
        (shuffle
         [[:reagent reagent-template render-reagent]
          [:crate crate-template render-append]
          [:jquery jquery-template render-append]
          [:react react-template render-react]
          [:sablono sablono-template render-react]])]
    (let [root (goog.dom/createDom "div")
          _ (goog.dom/append (body) root)
          secs (run-test root data li-fn render-fn)]
      [key secs])))

(deftest perf-test []
  (let [data (doall (gen-data))]
    (prn (->> (for [i (range 3)]
                (into {} (time-test data)))
              (reduce (partial merge-with +))
              (map (fn [[k v]] [k (/ v 3)]))
              (into {})))))
