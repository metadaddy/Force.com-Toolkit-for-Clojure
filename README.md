Clojure toolkit for [Force.com REST API](http://wiki.developerforce.com/page/REST_API). 

This project uses [Moritz Heidkamp](https://github.com/DerGuteMoritz)'s [clj-oauth2](https://github.com/DerGuteMoritz/clj-oauth2).

Deployment
----------

Create a connected app (Setup | App Setup | Create | Apps | Connected Apps | New). In older Developer Edition orgs, this is known as a remote access app (Setup | App Setup | Develop | Remote Access).

Running on localhost
--------------------

The sample Clojure app expects environment variables like so:

    export LOGIN_URI="https://login.salesforce.com"
    export REDIRECT_URI="http://localhost:8080/oauth/callback"
    export CLIENT_ID="YOUR_CONSUMER_KEY"
    export CLIENT_SECRET="YOUR_CONSUMER_SECRET"
    export DEBUG="true"
    export PORT="8080"
    
You can run without SSL with the above configuration, or configure an SSL port and keystore:
    
    export SSL_PORT="8080"
    export KEYSTORE="my.keystore"
    export KEY_PASSWORD="password"
    
Assuming you have [Leiningen](https://github.com/technomancy/leiningen) installed, you should be able to run the sample app with

    lein run

Running on Heroku
-----------------

A Procfile is included for Heroku. You will need to configure the environment thus:

    heroku config:add LOGIN_URI="https://login.salesforce.com" \
        REDIRECT_URI="https://yourapp.herokuapp.com/oauth/callback" \
        CLIENT_ID="YOUR_CONSUMER_KEY" \
        CLIENT_SECRET="YOUR_CONSUMER_SECRET" \
        DEBUG="true"