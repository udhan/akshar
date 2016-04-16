(defproject akshar "0.1.0-SNAPSHOT"
  :description "Simple text editor written in clojure"
  :url "http://udhan.com"
  :license {:name "MIT License"
            :url ""}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [me.raynes/fs "1.4.6"]]
  :main ^:skip-aot akshar.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
