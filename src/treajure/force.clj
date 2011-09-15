(ns treajure.force
    "Force.com API built on treasure.core"
    (:require [ring.util.codec :as codec])
    (:use (treajure core)))

; Macros currently get oauth token from current scope
; Can get oauth token from session - (:treajure.core/oauth ~'session)

(defmacro id
    []
    `(id-helper ~'oauth))

(defn id-helper
  "Given an OAuth response, return the id"
  [oauth]
  (raw-oauth-request 
      oauth
      (:id @oauth)))
      
(defmacro describe
    [obj-type]
    `(describe-helper ~'oauth ~obj-type))

(defn describe-helper
  "Given an OAuth response, and object type, return the sobject metadata"
  [oauth obj-type]
  (oauth-request 
      oauth
      (str "/services/data/v22.0/sobjects/" obj-type "/describe/")))
                      
(defmacro query
  [query]
  `(query-helper ~'oauth ~query))

(defn query-helper
    "Given an OAuth response, execute the supplied query and return the response"
    [oauth query]
    (oauth-request 
        oauth
        (str "/services/data/v22.0/query?q=" (codec/url-encode query))))

(defmacro create
  [obj-type fields]
  `(create-helper ~'oauth ~obj-type ~fields))

(defn create-helper 
    "Given an OAuth response, object type and fields, create the sobject, return status"
    [oauth obj-type fields]
    (oauth-request 
        oauth
        (str "/services/data/v22.0/sobjects/" obj-type)
        "POST"
        fields))
            			  
(defmacro retrieve
    ([obj-type id]
    `(retrieve-helper ~'oauth ~obj-type ~id))
    ([obj-type id field-list]
    `(retrieve-helper ~'oauth ~obj-type ~id ~field-list)))

(defn retrieve-helper
    "Given an OAuth response, object type, id and optional comma-separated field list, return the sobject"
    ([oauth obj-type id]
    (oauth-request 
        oauth
        (str "/services/data/v22.0/sobjects/" obj-type "/" id)))
    ; TODO - make field list a vector?
    ([oauth obj-type id field-list]
    (oauth-request 
        oauth
        (str "/services/data/v22.0/sobjects/" obj-type "/" id "?fields=" field-list))))

(defmacro update
  [obj-type id fields]
  `(update-helper ~'oauth ~obj-type ~id ~fields))

(defn update-helper 
    "Given an OAuth response, object type and fields, update the sobject"
    ; Note - Java HttpURLConnection does not support PATCH (at least as of 
    ; Java SE 6), so use the _HttpMethod workaround
    [oauth obj-type id fields]
    (oauth-request 
        oauth
        (str "/services/data/v22.0/sobjects/" obj-type "/" id "?_HttpMethod=PATCH")
        "POST"
        fields))

(defmacro upsert
  [obj-type externalIdField externalId fields]
  `(upsert-helper ~'oauth ~obj-type ~externalIdField ~externalId ~fields))

(defn upsert-helper 
    "Given an OAuth response, object type, external id field, external id and 
    fields, upsert the sobject"
    ; Note - Java HttpURLConnection does not support PATCH (at least as of 
    ; Java SE 6), so use the _HttpMethod workaround
    [oauth obj-type externalIdField externalId fields]
    (oauth-request 
        oauth
        (str "/services/data/v22.0/sobjects/" obj-type "/" externalIdField "/" externalId "?_HttpMethod=PATCH")
        "POST"
        fields))

(defmacro delete
    [obj-type id]
    `(delete-helper ~'oauth ~obj-type ~id))

(defn delete-helper
    "Given an OAuth response, object type and id, delete the sobject"
    [oauth obj-type id]
    (oauth-request 
      oauth
      (str "/services/data/v22.0/sobjects/" obj-type "/" id)
      "DELETE"))
