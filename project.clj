(defproject clojure-force "1.0.0-SNAPSHOT"
  :description "FIXME: write description"
  :dependencies [[org.clojure/clojure "1.3.0"]
	 							 [org.clojure/data.json "0.1.1"]
                 [compojure "1.0.1"]
                 [ring/ring-jetty-adapter "1.0.2"]
								 [clj-http "0.2.6"] ; for clj-oauth2
								 [uri "1.1.0"] ; for clj-oauth2
                 [commons-codec/commons-codec "1.6"] ; for clj-oauth2
								 [clj-oauth2 "0.3.1"]
                 [enlive "1.0.0"]]
  :repositories {"local" ~(str (.toURI (java.io.File. "maven_repository")))}
  :main clojure-force.core)
