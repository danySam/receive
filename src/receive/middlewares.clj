(ns receive.middlewares
  (:require
   [clojure.set :refer [rename-keys]]
   [clojure.string :as string]
   [clojure.tools.logging :as log]
   [clojure.walk :refer [keywordize-keys]]
   [receive.error-handler :refer [not-error?]]
   [receive.auth.jwt :as jwt]))

(defn wrap-fallback-exception
  [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception e
        (log/error e)
        {:status 500 :body {:success false
                            :message "Server error"}}))))

(defn wrap-postgres-exception
  [handler]
  (fn [request]
    (try
      (handler request)
      (catch org.postgresql.util.PSQLException e
        (log/error e)
        {:status 400
         :body {:success false
                :message "Invalid data"}}))))

(defn trim-trailing-slash [uri]
  (when (and (not= uri "/")
             (.endsWith uri "/"))
    (-> uri drop-last string/join)))

(defn wrap-with-uri-rewrite [handler f]
  (fn [{uri :uri :as request}]
    (if-let [rewrite (f uri)]
      (handler (assoc request :uri rewrite))
      (handler request))))

(defn verified-user
  "Adds :auth data to request if user is authenticated
   :auth in request will be used throughout the application for authorization
   :auth is nil if no JWT token is provided by the client"
  [handler]
  (fn [request]
    (let [access-token (-> request
                           :cookies
                           :access_token
                           :value)
          verified-token (jwt/verify access-token)
          auth (when (not-error? verified-token) verified-token)]
      (handler (assoc request
                      :auth (rename-keys auth {:user_id :user-id}))))))

(defn wrap-cookies-keyword
  "Converts :cookies in request to keywordized map"
  [handler]
  (fn [request]
    (handler (assoc request
                    :cookies
                    (keywordize-keys (:cookies request))))))