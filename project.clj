(defproject com.brunobonacci/dragonfiles "0.1.0-SNAPSHOT"
  :description "A Clojure scriptable file processor (awk on steroids)"
  :url "http://example.com/FIXME"
  :license {:name "MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :javac-options ["-target" "1.7" "-source" "1.7" "-Xlint:-options"]

  :dependencies [
                 [org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.cli "0.3.1"]
                 [com.taoensso/timbre "3.4.0"]
                 [me.raynes/fs "1.4.6"]
                 [cheshire "5.4.0"]]

  :main dragonfiles.main

  :profiles {:uberjar {:aot :all}
             :dev {:dependencies [[midje "1.6.3"]]
                   :plugins [[lein-midje "3.1.3"]
                             [lein-bin "0.3.5"]]}}

  :jvm-opts ["-server" "-Dfile.encoding=utf-8"]
  :bin {:name "dgf" :bootclasspath false})
