(defproject clojure-force-sample "1.0.0"
  :description "Force.com REST API Sample in Clojure"
  :dependencies [[org.clojure/clojure "1.2.0"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [compojure "0.4.1"]
                 [ring/ring-jetty-adapter "0.2.5"]
                 [enlive "1.0.0"]]
  :dev-dependencies [[lein-run "1.0.0"]
                     [ring/ring-devel "0.2.5"]]
  :main sample.app)
