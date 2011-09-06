(ns sample.app
  (:use (ring.adapter jetty)
        (ring.middleware session)
        (compojure core)
        (treajure core force))
  (:require [compojure.route :as route]
			[net.cgrand.enlive-html :as html]))

; Options for treajure
(def oauth-params {:login-uri (get (System/getenv) "LOGIN_URI" "https://login.salesforce.com")
                   :redirect-uri (System/getenv "REDIRECT_URI") ; TODO - figure out default
                   :client-id (System/getenv "CLIENT_ID")
                   :client-secret (System/getenv "CLIENT_SECRET")
                   :trace-messages (Boolean/valueOf (get (System/getenv) "DEBUG" "false"))
                   })

; HTML templating stuff                  
(def *field-sel* [:select#field :> html/first-child])

(html/defsnippet field-model "templates/index.html" *field-sel*
  [{name :name label :label}]
  [:option] (html/do->
        (html/content label)
        (html/set-attr :value name)))

(def *link-sel* [:table.accountlist :> html/first-child])
    
(html/defsnippet link-model "templates/index.html" *link-sel*
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
    [:span#id] (html/content (get response :id)))

(html/deftemplate deleted "templates/deleted.html"
    [id]
    [:span#id] (html/content id))

(html/deftemplate updated "templates/updated.html"
    [id]
    [:span#id] (html/content id))

(html/deftemplate logout "templates/logout.html"
    [_])

(defn render 
    "Helper function for rendering Enlive output"
    [t] (apply str t))

; This is the mapping of URL paths to actions
(defroutes main-routes
  (GET "/" 
    {params :params session :session oauth :oauth} 
	(let [field-list (if (nil? (:field-list session))
    	    (filter #(= (:type %) "string") (get (describe "Account") :fields))
    	    (:field-list session))
	    user-id (if (nil? (:user-id session))
    	    (id)
    	    (:user-id session))]
		{:headers {"Content-type" "text/html; charset=UTF-8"}
		 :session {:field-list field-list :user-id user-id}
		 :body (render (index (:display_name user-id) field-list
	         (get (if 
	             (nil? (params "value")) 
	             (query (str "SELECT Name, Id FROM Account ORDER BY Name LIMIT 20"))
	             (query (str "SELECT Name, Id FROM Account WHERE " (params "field") " LIKE '" (params "value") "%' ORDER BY Name LIMIT 20")))
			      :records)))}))
  (GET "/logout" 
	{params :params oauth :oauth} 
	{:status 302
	 :headers {"Content-type" "text/html; charset=UTF-8", 
	    "Location" "https://superpat-developer-edition.my.salesforce.com/secur/logout.jsp"}
	 :oauth nil})
  (GET "/detail" 
	{params :params oauth :oauth} 
	{:headers {"Content-type" "text/html; charset=UTF-8"}
	 :body 
		(let [account (retrieve
		    "Account" 
		    (params "id") 
		    "Name,Industry,TickerSymbol")] 
			(render (detail account)))})
  (POST "/action"
  	{params :params oauth :oauth} 
  	(cond
          (not (nil? (params "new"))) 
          {:headers {"Content-type" "text/html; charset=UTF-8"}
      	 :body (render (new-account))}
          (not (nil? (params "delete"))) 
          {:headers {"Content-type" "text/html; charset=UTF-8"}
      	 :body (do (delete 
     		    "Account" 
     		    (params "id"))
     			(render (deleted (params "id"))))}
          (not (nil? (params "edit"))) 
          {:headers {"Content-type" "text/html; charset=UTF-8"}
        	 :body 
        		(let [account (retrieve 
        		    "Account" 
        		    (params "id") 
        		    "Name,Industry,TickerSymbol")] 
        			(render (edit account)))}))
  (POST "/account"
	{params :params oauth :oauth} 
	(cond
        (not (nil? (params "create"))) 
        {:headers {"Content-type" "text/html; charset=UTF-8"}
    	 :body (let [response (create 
   		    "Account" 
   		    ; TODO - compose this map dynamically?
   		    {:Name (params "Name") :Industry (params "Industry") :TickerSymbol (params "TickerSymbol")})] 
   			(render (created response)))}
        (not (nil? (params "update"))) 
        {:headers {"Content-type" "text/html; charset=UTF-8"}
      	 :body (do (update
  		    "Account" 
  		    (params "id")
   		    ; TODO - compose this map dynamically?
   		    {:Name (params "Name") :Industry (params "Industry") :TickerSymbol (params "TickerSymbol")})  
  			(render (updated (params "id"))))}))
  (route/files "/" {:root "www/public"})
  (route/not-found "Page not found"))

; This is the magic - wrap our routes with the treajure middleware
(wrap! main-routes (:oauth oauth-params)) 

(defn -main []
  (let [port (Integer/parseInt (get (System/getenv) "PORT" "8080"))]
  (if-let [ssl-port (Integer/parseInt (System/getenv "SSL_PORT"))]
    (run-jetty main-routes {:join? false :ssl? true :port port :ssl-port ssl-port
                            :keystore (System/getenv "KEYSTORE")
                            :key-password (System/getenv "KEY_PASSWORD")})
    (run-jetty main-routes {:port port}))))
