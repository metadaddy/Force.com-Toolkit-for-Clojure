Very embryonic Clojure toolkit for Force.com REST API. Brace yourself for rapid evolution!

Create a remote access app (Setup | App Setup | Develop | Remote Access).

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