(ns clojure-force.core
	(:use (clojure-force force)
	      [clojure.pprint :only [pprint]]
				[clojure.walk :only [keywordize-keys]]
				[compojure.core :only [defroutes GET POST]]
				[ring.middleware.session :only [wrap-session]]
				[ring.middleware.params :only [wrap-params]]
		    [ring.middleware.keyword-params :only [wrap-keyword-params]])
	(:require [ring.adapter.jetty :as jetty]
		        [clj-oauth2.client :as oauth2]
		        [clj-oauth2.ring :as oauth2-ring]
		        [compojure.route :as route]
						[net.cgrand.enlive-html :as html]))
		
; oauth2 2.0 Parameters for Force.com
(def login-uri
	(get (System/getenv) "LOGIN_URI" "https://login.salesforce.com"))
	
(defn my-excluded [uri]
	(contains? #{"/exclude1" "/exclude2"} uri))

(def force-com-oauth2
  {:authorization-uri (str login-uri "/services/oauth2/authorize")
   :access-token-uri (str login-uri "/services/oauth2/token")
   :redirect-uri (System/getenv "REDIRECT_URI") ; TODO - figure out default
   :client-id (System/getenv "CLIENT_ID")
   :client-secret (System/getenv "CLIENT_SECRET")
   :scope ["id" "api" "refresh_token"]
   :grant-type "authorization_code"
   :force-https (System/getenv "FORCE_HTTPS") ; on Heroku the app thinks it is always http
   :trace-messages (Boolean/valueOf (get (System/getenv) "DEBUG" "false"))
   :get-state oauth2-ring/get-state-from-session
   :put-state oauth2-ring/put-state-in-session
   :get-target oauth2-ring/get-target-from-session
   :put-target oauth2-ring/put-target-in-session
   :get-oauth2-data oauth2-ring/get-oauth2-data-from-session
   :put-oauth2-data oauth2-ring/put-oauth2-data-in-session
   :exclude #"^/exclude.*"})

; HTML templating stuff                  
(def field-sel [:select#field :> html/first-child])

(html/defsnippet field-model "templates/index.html" field-sel
  [{name :name label :label}]
  [:option] (html/do->
        (html/content label)
        (html/set-attr :value name)))

(def link-sel [:table.accountlist :> html/first-child])
    
(html/defsnippet link-model "templates/index.html" link-sel
  [{name :Name id :Id}]
  [:a] (html/do->
        (html/content name)
        (html/set-attr :href (str "detail?id=" id))))
    
(html/deftemplate index "templates/index.html"
    [display-name fields accounts]
    [:span#displayname] (html/content display-name)
    [:select#field] (html/content (map field-model fields))
    [:table.accountlist] (html/content (map link-model accounts)))

(html/deftemplate detail "templates/detail.html"
    [account]
    [:td#accountname] (html/content (get account :Name))
    [:a#industry] (html/do->
                    (html/content (get account :Industry))
                    (html/set-attr :href (str "/?field=Industry&value=" (get account :Industry) "&search=Search")))
    [:td#tickersymbol] (html/content (get account :TickerSymbol))
    [:input#id] (html/set-attr :value (get account :Id)))

(html/deftemplate edit "templates/edit.html"
    [account]
    [:input#Name] (html/set-attr :value (get account :Name))
    [:input#Industry] (html/set-attr :value (get account :Industry))
    [:input#TickerSymbol] (html/set-attr :value (get account :TickerSymbol))
    [:input#id] (html/set-attr :value (get account :Id)))

(html/deftemplate new-account "templates/edit.html"
    []
    [:h1#header] (html/set-attr :value "New Account")
    [:input#Name] (html/set-attr :value "")
    [:input#Industry] (html/set-attr :value "")
    [:input#TickerSymbol] (html/set-attr :value "")
    [:input#id] (html/set-attr :value "")
    [:input#action] 
    (html/do->
        (html/set-attr :value "Create") 
        (html/set-attr :name "create")))

(html/deftemplate created "templates/created.html"
    [response]
    [:span#id] (html/content (:id response)))

(html/deftemplate deleted "templates/deleted.html"
    [id]
    [:span#id] (html/content id))

(html/deftemplate updated "templates/updated.html"
    [id]
    [:span#id] (html/content id))

(html/deftemplate logout "templates/logout.html"
    [instance-url]
    [:iframe#logoutframe] (html/set-attr :src (str instance-url "/secur/logout.jsp")))

(defn render 
    "Helper function for rendering Enlive output"
    [t] (apply str t))

; This is the mapping of URL paths to actions
(defroutes handler
  (GET "/" 
    {params :params session :session oauth2 :oauth2} 
	(let [field-list (if (nil? (:field-list session))
    	    (filter #(= (:type %) "string") (:fields (describe "Account")))
    	    (:field-list session))
	    user-id (if (nil? (:user-id session))
    	    (id)
    	    (:user-id session))]
		{:headers {"Content-type" "text/html; charset=UTF-8"}
		 :session {:field-list field-list :user-id user-id}
		 :body (render (index (:display_name user-id) field-list
	         (get (if 
	             (nil? (:value params)) 
	             (query "SELECT Name, Id FROM Account ORDER BY Name LIMIT 20")
	             (query (str "SELECT Name, Id FROM Account WHERE " (:field params) " LIKE '" (:value params) "%' ORDER BY Name LIMIT 20")))
			      :records)))}))
	(GET "/test" 
    {oauth2 :oauth2} 
		(:body (oauth2/get 
			       (str 
	             (:instance_url (:params oauth2)) 
	             "/services/data/v24.0/sobjects/Account/0015000000VALDxAAP")
	           {:oauth2 oauth2})))
  (GET "/test2" 
    {oauth2 :oauth2} 
 	  (with-out-str 
      (pprint (retrieve "Account" "0015000000VALDxAAP"))))
  (GET "/logout" 
	  {oauth2 :oauth2}
	  {:oauth2 nil
	   :body (render (logout (:instance_url (:params oauth2))))})
  (GET "/detail" 
	{params :params oauth2 :oauth2} 
	{:headers {"Content-type" "text/html; charset=UTF-8"}
	 :body 
		(let [account (retrieve
		    "Account" 
		    (:id params) 
		    "Name,Industry,TickerSymbol")] 
			(render (detail account)))})
  (POST "/action"
  	{params :params oauth2 :oauth2} 
  	(cond
          (not (nil? (:new params))) 
          {:headers {"Content-type" "text/html; charset=UTF-8"}
      	 :body (render (new-account))}
          (not (nil? (:delete params))) 
          {:headers {"Content-type" "text/html; charset=UTF-8"}
      	 :body (do (delete 
     		    "Account" 
     		    (:id params))
     			(render (deleted (:id params))))}
          (not (nil? (:edit params))) 
          {:headers {"Content-type" "text/html; charset=UTF-8"}
        	 :body 
        		(let [account (retrieve "Account" (:id params) "Name,Industry,TickerSymbol")] 
        			(render (edit account)))}))
  (POST "/account"
		{params :params oauth2 :oauth2} 
		(cond
      (not (nil? (:create params))) 
        {:headers {"Content-type" "text/html; charset=UTF-8"}
    	   :body (let [response (create "Account" (dissoc params :create :id))] 
   			(render (created response)))}
      (not (nil? (:update params))) 
        {:headers {"Content-type" "text/html; charset=UTF-8"}
      	 :body (do (update "Account" (:id params) (dissoc params :update :id))  
  			(render (updated (:id params))))}))
  (GET "/exclude1" []
		{:headers {"Content-type" "text/html; charset=UTF-8"}
		 :body "excluded1"})
  (GET "/exclude2" []
		{:headers {"Content-type" "text/html; charset=UTF-8"}
		 :body "excluded2"})
  (route/files "/" {:root "www/public"})
  (route/not-found "Page not found"))

; Set up the middleware
(def app 
	(-> handler 
		(oauth2-ring/wrap-oauth2 force-com-oauth2)
		wrap-session 
		wrap-keyword-params
		wrap-params))

(defn -main []
  (let [port (Integer/parseInt (get (System/getenv) "PORT" "8080"))]
	  (if-let [ssl-port-str (System/getenv "SSL_PORT")]
	    (jetty/run-jetty app {:join? false :ssl? true :port port :ssl-port (Integer/parseInt ssl-port-str)
	                            :keystore (System/getenv "KEYSTORE")
	                            :key-password (System/getenv "KEY_PASSWORD")})
	    (jetty/run-jetty app {:port port}))))