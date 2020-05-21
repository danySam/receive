(ns receive.routes
  (:require [bidi.ring :refer [make-handler]]
            [receive.handlers.api :as api-handlers]
            [receive.handlers.file :as file-handlers]
            [receive.middlewares :refer [wrap-fallback-exception
                                         wrap-postgres-exception
                                         wrap-with-uri-rewrite
                                         trim-trailing-slash
                                         verified-user
                                         wrap-cookies-keyword]]
            [ring.middleware.cookies :refer [wrap-cookies]]
            [ring.middleware.json :refer [wrap-json-response
                                          wrap-json-params]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.multipart-params :refer [wrap-multipart-params]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.logger :refer [wrap-with-logger]]))

(def api-routes
  {"/ping" (wrap-json-response api-handlers/ping)
   "/download/" {[:id ""] file-handlers/download-file}
   "/upload" {:post {"" (-> file-handlers/upload
                            (wrap-json-response))}}
   "/signup" {:post (-> api-handlers/signup
                        (wrap-json-response))}})

(def routes
  ["/" {"" file-handlers/index
        "api" api-routes
        "download/"  {[:id ""] file-handlers/download-view}
        "share" {:get file-handlers/share-handler}
        true (wrap-json-response api-handlers/not-found)}])

(def handler
  (-> routes
      (make-handler)
      (verified-user)
      (wrap-cookies-keyword)
      (wrap-cookies)
      (wrap-keyword-params)
      (wrap-params)
      (wrap-json-params)
      (wrap-postgres-exception)
      (wrap-fallback-exception)
      (wrap-multipart-params)
      (wrap-with-uri-rewrite trim-trailing-slash)
      (wrap-resource "public")
      (wrap-with-logger)))