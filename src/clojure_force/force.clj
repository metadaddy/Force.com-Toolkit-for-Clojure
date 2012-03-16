(ns clojure-force.force
    "Force.com API built on treasure.core"
		(:use [clj-oauth2.client :only [request]])
    (:require [ring.util.codec :as codec]
	            [clojure.data.json :as json]))

; Macros currently get oauth2 token from current scope
; Can get oauth2 token from session - (:treajure.core/oauth2 ~'session)

(defn force-request
		[url opts]
		(do 
			 (when (:trace-messages (:oauth2 opts)) 
				 (println (str "clojure-force.force.force-request url: " url))
			   (println (str "clojure-force.force.force-request body: " (:body opts))))
			 (let [raw-response (request (merge {:url url :throw-exceptions false :content-type :json :accept :json} opts))]
					(when (:trace-messages (:oauth2 opts)) 
					  (println (str "clojure-force.force.force-request raw-response: " raw-response)))
					(let [json-response (:body raw-response)]
					     (let [response (if (or (nil? json-response) (== (count json-response) 0)) nil (json/read-json json-response))]
						   (when (:trace-messages (:oauth2 opts)) 
							   (println (str "clojure-force.force.force-request cooked response: " response)))
						   response)))))
      
(defmacro id
    []
    `(id-helper ~'oauth2))

(defn id-helper
  "Given an oauth2 response, return the id"
  [oauth2]
  (force-request
      (:id (:params oauth2))
      {:method :get :oauth2 oauth2}))

(defmacro describe
    [obj-type]
    `(describe-helper ~'oauth2 ~obj-type))

(defn describe-helper
  "Given an oauth2 response, and object type, return the sobject metadata"
  [oauth2 obj-type]
  (force-request
      (str (:instance_url (:params oauth2)) "/services/data/v24.0/sobjects/" obj-type "/describe/")
      {:method :get :oauth2 oauth2}))
                      
(defmacro query
  [query]
  `(query-helper ~'oauth2 ~query))

(defn query-helper
    "Given an oauth2 response, execute the supplied query and return the response"
    [oauth2 query]
    (force-request
        (str (:instance_url (:params oauth2)) "/services/data/v24.0/query?q=" (codec/url-encode query))
        {:method :get :oauth2 oauth2}))

(defmacro create
  [obj-type fields]
  `(create-helper ~'oauth2 ~obj-type ~fields))

(defn create-helper 
    "Given an oauth2 response, object type and fields, create the sobject, return status"
    [oauth2 obj-type fields]
    (force-request (str (:instance_url (:params oauth2)) "/services/data/v24.0/sobjects/" obj-type)
        {:method :post 
	       :oauth2 oauth2 
	       :body (json/json-str fields)}))
            			  
(defmacro retrieve
    ([obj-type id]
    `(retrieve-helper ~'oauth2 ~obj-type ~id))
    ([obj-type id field-list]
    `(retrieve-helper ~'oauth2 ~obj-type ~id ~field-list)))

(defn retrieve-helper
    "Given an oauth2 response, object type, id and optional comma-separated field list, return the sobject"
    ([oauth2 obj-type id]
    (force-request
        (str (:instance_url (:params oauth2)) "/services/data/v24.0/sobjects/" obj-type "/" id)
        {:method :get :oauth2 oauth2}))
    ; TODO - make field list a vector?
    ([oauth2 obj-type id field-list]
    (force-request
        (str (:instance_url (:params oauth2)) "/services/data/v24.0/sobjects/" obj-type "/" id "?fields=" field-list)
        {:method :get :oauth2 oauth2})))

(defmacro update
  [obj-type id fields]
  `(update-helper ~'oauth2 ~obj-type ~id ~fields))

(defn update-helper 
    "Given an oauth2 response, object type and fields, update the sobject"
    ; Note - Java HttpURLConnection does not support PATCH (at least as of 
    ; Java SE 6), so use the _HttpMethod workaround
    [oauth2 obj-type id fields]
    (force-request
        (str (:instance_url (:params oauth2)) "/services/data/v24.0/sobjects/" obj-type "/" id "?_HttpMethod=PATCH")
        {:method :post 
	       :oauth2 oauth2 
	       :body (json/json-str fields)}))

(defmacro delete
    [obj-type id]
    `(delete-helper ~'oauth2 ~obj-type ~id))

(defn delete-helper
    "Given an oauth2 response, object type and id, delete the sobject"
    [oauth2 obj-type id]
    (force-request
      (str (:instance_url (:params oauth2)) "/services/data/v24.0/sobjects/" obj-type "/" id)
      {:method :delete :oauth2 oauth2}))