(ns receive.handlers.file
  (:require [clojure.java.io :as io]
            [hiccup.core :as h]
            [receive.config :refer [config]]
            [receive.error-handler :refer [error?
                                           error->http-response
                                           error->ui-response]]
            [receive.service.files :as files]
            [receive.spec.file :as spec]
            [receive.view.base
             :refer [base upload-button
                     toolbar download-button
                     copy-button
                     error-ui]
             :rename {base base-layout}])
  (:import java.util.UUID))

(defn uuid-str []
  (str (UUID/randomUUID)))

(def response-no-file-uploaded
  {:status 400
   :body {:success false
          :message "No file uploaded"}})

(def response-file-too-large
  {:status 413
   :body {:success false
          :message "File too big"}})

(def response-file-not-provided
  {:status 400
   :body {:success false
          :message "File not provided"}})

(def response-filename-too-long
  {:status 400
   :body {:success false
          :message "File name is too long"}})

(def response-bad-uid
  {:status 400
   :body {:success false
          :messsage "Not valid UUID"}})

(defn upload
  "Handles file upload and saves to the location specified in the config"
  [{params :params :as request}]
  (cond
    (not (spec/params-valid? params)) response-no-file-uploaded
    (not (spec/max-file-size-valid? params)) response-file-too-large
    (not (spec/min-file-size-valid? params)) response-file-not-provided
    (not (spec/max-filename-length-valid? params)) response-filename-too-long
    :else
    (let [file (-> request :params :file)
          tempfile (:tempfile file)
          filename (:filename file)
          result (files/save-file tempfile filename)]
      {:status 200
       :body {:name filename
              :uid result
              :success true
              :message "File saved successfully!"}})))

(defn download-file
  [{params :params}]
  (cond
    (not (spec/uuid-valid? params)) response-bad-uid
    :else (let [uid (:id params)
                abs-filename (files/get-absolute-filename uid)]
            (if (error? abs-filename)
              (error->http-response abs-filename)
              {:status 200
               :body (io/file abs-filename)}))))

(defn error-body-builder [status message]
  (h/html (base-layout [:div
                        (error-ui status message)])))

(defn download-view [{params :params :as request}]
  (cond
    (not (spec/uuid-valid? params)) response-bad-uid
    :else
    (let [uid (:id params)
          filename (files/get-filename uid)]
      (if (error? filename)
        (error->ui-response filename error-body-builder)
        {:status 200
         :headers {"Content-Type" "text/html"}
         :body (h/html
                (base-layout [:div
                              (toolbar (:auth request))
                              (download-button uid filename)]))}))))

(defn index [request]
  (let [auth (:auth request)]
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body (h/html (base-layout [:div
                                 (toolbar auth)
                                 upload-button]))}))

(defn download-link
  [uid]
  (format "%s/download/%s/" (:base-url config) uid))

(defn share-handler [request]
  (let [uid (-> request :params :uid)
        link (download-link uid)
        auth (:auth request)]
    {:status 200
     :body (h/html (base-layout [:div
                                 (toolbar auth)
                                 (copy-button link)]))}))