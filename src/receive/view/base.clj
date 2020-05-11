(ns receive.view.base
  (:require [hiccup.page :as page]
            [receive.config :as config]))

(defn base [children]
  (page/html5
   [:head
    [:base {:href (:base-url config/config) :target "_blank"}]
    [:title "Receive UI"]
    [:title (:ui-title config/config)]
    [:meta {:charset "utf-8"}]
    [:meta {:name "theme-color" :content "#5CDb95"}]
    (page/include-css "css/style.css")]
   [:body (if config/staging? {:class "env-staging"} {})
    [:div {:class "container"}
     children
     (page/include-js "js/main.js")]]))

(def title
  [:a {:href (:base-url config/config)}
   [:div {:class "title-section"}
    "> Receive"]])

(def upload-button
  [:form {:action "/upload/"
          :method "post"
          :enctype "multipart/form-data"
          :name "myForm"}
   [:div {:class "upload-input" :onclick "getFile()"} "Upload"]
   [:div {:style "height: 0px; width: 0px; overflow: hidden"}
    [:input {:id "upfile" :type "file" :name "file" :value "file" :onchange "uploadFile(this)"}]]])

(defn download-link [uid]
  (format "/download/api/%s/" uid))

(defn download-button [uid filename]
  [:a {:download filename :href (download-link uid)}
   [:button "Download"]
   [:p filename]])