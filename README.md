Very embryonic Clojure toolkit for Force.com REST API. Brace yourself for rapid evolution!

Create a remote access app (Setup | App Setup | Develop | Remote Access).

The sample Clojure app expects environment variables like so:

    export LOGIN_URI="https://login.salesforce.com"
    export REDIRECT_URI="http://localhost:8080/oauth/callback"
    export CLIENT_ID="YOUR_CONSUMER_KEY"
    export CLIENT_SECRET="YOUR_CONSUMER_SECRET"
    export DEBUG="true"
    export PORT="8080"
    export KEYSTORE="my.keystore"
    export KEY_PASSWORD="password"
    
Assuming you have [Leiningen](https://github.com/technomancy/leiningen) installed, you should be able to run the sample app with

    lein run

