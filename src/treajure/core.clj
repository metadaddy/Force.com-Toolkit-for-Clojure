(ns treajure.core
    "OAuth Ring Middleware. This should be fairly generic OAuth 2 draft 10."
    (:require [ring.middleware.params :as params]
              [ring.middleware.session :as session]
              [ring.util.codec :as codec]
    	      [clojure.contrib.http.agent :as http-agent]
    		  [clojure.contrib.java-utils :as java-utils]
              [clojure.contrib.json :as json]))

(defn- get-oauth-service-url 
  "Get the OAuth authentication service url to which the user should be
  redirected to authenticate and authorize the app."
  [oauth-params]
  (str 
  	(:login-uri oauth-params) "/services/oauth2/authorize?response_type=code" 
  	"&client_id=" (:client-id oauth-params)
  	"&redirect_uri=" (codec/url-encode (:redirect-uri oauth-params))))

(defn- refresh-oauth 
    "Do the OAuth refresh flow and return a map comprising a merge of the new
    access token etc with the old client id, secret etc"
    [oauth]
    (when (:trace-messages oauth) 
        (println (str "Sending OAuth request for refresh token " (:refresh-token oauth))))
	(let [response 	
		(json/read-json (http-agent/string
    		(http-agent/http-agent (str (:login-uri oauth) "/services/oauth2/token")
    		  :method "POST" 
    		  :body (str
    		    "grant_type=refresh_token" 
    		    "&client_id=" (:client-id oauth) 
    		    "&client_secret=" (:client-secret oauth) 
    		    "&refresh_token=" (:refresh_token oauth)))))]
            (when (:trace-messages oauth) 
                (println (str "OAuth GET response is " response)))
        	(merge oauth response)))

(defn- get-oauth 
    "Exchange an OAuth authorization code for an OAuth response containing the
    instance url, access token etc and the incoming configuration parameters"
    [oauth-params code]
    (when (:trace-messages oauth-params) 
        (println (str "Sending OAuth request for code " code)))
	(let [response 	
		(json/read-json (http-agent/string
    		(http-agent/http-agent (str (:login-uri oauth-params) "/services/oauth2/token")
    		  :method "POST" 
    		  :body (str
    		 	"code=" code
    		    "&grant_type=authorization_code" 
    		    "&client_id=" (:client-id oauth-params) 
    		    "&client_secret=" (:client-secret oauth-params) 
    		    "&redirect_uri=" (:redirect-uri oauth-params)))))]
            (when (:trace-messages oauth-params) 
                (println (str "OAuth GET response is " response)))
        	(merge oauth-params response)))

(defn raw-oauth-request
    "Low level OAuth request function"
    ([oauth url] (raw-oauth-request oauth url "GET" nil false))
    ([oauth url method] (raw-oauth-request oauth url method nil false))
    ([oauth url method body] (raw-oauth-request oauth url method nil false))
    ([oauth url method body retry]
        (when (:trace-messages @oauth) 
            (println (str "Sending OAuth " method 
                " Access token " (:access_token @oauth) 
                " url " url 
                (if (nil? body) " no body" (str " body " body)))))
        (let [http-agnt (http-agent/http-agent url
          		  :method method
          		  :headers {"Authorization" (str "OAuth " (:access_token @oauth)) "Content-Type" "application/json"}
          		  :body (if (nil? body) 
          		    nil
          		    (json/json-str body)))
              json-response  (http-agent/string http-agnt)
          	  status (http-agent/status http-agnt)
          	  response (if (or (nil? json-response) (== (count json-response) 0)) nil (json/read-json json-response))]
          	(if (and (= status 401) (not retry))
          	  (do
                (when (:trace-messages @oauth) 
                  (println "Refreshing token"))
          	    (swap! oauth refresh-oauth)
          	    ; Try again, setting retry to true
          	    (raw-oauth-request oauth url method body true))
          	  (do 
          	    (when (:trace-messages @oauth) 
                  (println (str "OAuth " method " status is " status (when (not (nil? response)) (str " response is " response)))))
                response)))))

(defn oauth-request
    "Low level OAuth request function - prepends instance URL"
    ([oauth url] (raw-oauth-request oauth (str (:instance_url @oauth) url) "GET" nil))
    ([oauth url method] (raw-oauth-request oauth (str (:instance_url @oauth) url) method nil))
    ([oauth url method body] (raw-oauth-request oauth (str (:instance_url @oauth) url) method body)))

(defn wrap-oauth
  "Handles the OAuth protocol."
  [handler oauth-params]
  (params/wrap-params
    (session/wrap-session
        (fn [request]
            ; Is the request uri path the same as the redirect URI path?
            (if (= (:uri request) (.getPath (java-utils/as-url (:redirect-uri oauth-params))))
            	; We should have an authorization code - get the access token 
            	; and put it in the session, along with the list of string 
            	; fields for Account
                {:status 302
            	:headers {"Location" "/"}
            	:session (let [oauth (get-oauth oauth-params ((:params request) "code"))]
            	            ; ::oauth is an atom so we can modify it when we
            	            ; refresh the access token
            	            {::oauth (atom oauth)})}
             	(let [oauth (::oauth (:session request))]
             		(if (nil? oauth) 
             			; Redirect to OAuth authentication/authorization
             			{:status 302 :headers {"Location" (get-oauth-service-url oauth-params)}}
             			; Put the OAuth response on the request and invoke
             			; handler
             			(if-let [response (handler (assoc request :oauth oauth))]
             			    (if-let [session (response :session)]
                 			    (if-let [new-oauth (find response :oauth)]
                 			        ; Handler has put data in the session, and set oauth
                 			        ; merge it all together
                 			        (assoc response :session (merge (response :session) {::oauth (:oauth response)}))
                 			        ; Handler has put data in the session - add 
                 			        ; the oauth data to it
                 			        (assoc response :session (merge (response :session) {::oauth oauth})))
                 			    (if-let [new-oauth (find response :oauth)]
                 			        ; Handler has set oauth, but not changed session - merge
                 			        ; the new oauth into the session from the request
                 			        (assoc response :session (merge (request :session) {::oauth (:oauth response)}))
                 			        ; No change to session - our oauth data will
                 			        ; be fine. If we were to set it here, we would 
                 			        ; wipe out the existing session state!
                 			        response))))))))))
