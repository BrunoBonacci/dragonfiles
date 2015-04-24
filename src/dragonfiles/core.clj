(ns dragonfiles.core
  (:require [clojure.java.io :as io])
  (:require [clojure.string :refer [trim] :as s ])
  (:require [clojure.tools.cli :refer [parse-opts]])
  (:require [me.raynes.fs :as fs])
  (:gen-class))



(defn process-file [source processor output]
  (print (str "(*) Exporting: " source " -> " output "\n"))

  (try
    ;; create parent directory if not exists
    (fs/mkdirs (.getParentFile (io/file output)))

    ;; finally processing file
    (->> source
         processor
         ((partial spit output)))

    (catch Exception x
      (binding [*out* *err*]
        (print (str "ERROR: processing " source ", reason: " x "\n"))))))



(defn target-file-name [root file output ext]
  (let [^String root (.getCanonicalPath (io/file root))
        ^String file (.getCanonicalPath (io/file file))
        ^String output (.getCanonicalPath (io/file output))]
    (-> file
        (.replaceFirst (str "^\\Q" root "\\E" ) output)
        (.replaceFirst (str "\\.[^.]*$") (str "." ext))
        io/file)))


(defn build-files-list [source output output-ext]
  (let [file-list      (filter #(.isFile %) (fs/find-files source #".*"))
        more-than-one (next file-list)]
    (if more-than-one
      (map (juxt identity #(target-file-name source % output output-ext)) file-list)
      [[(first file-list) (io/file output)]])))


(defn process-files [source processor output parallel?]
  (let [files (build-files-list source output "txt")
        mapper (if parallel? #'pmap #'map)]
    (doall
     (mapper (fn [[src out]] (process-file src processor out)) files))))


(defn prepare-script [script]
  (binding [*ns* (create-ns 'user)]
    ;; requiring standard namespaces
    (require '[clojure.string :as s :refer [split]])
    (require '[clojure.java.io :as io])
    (require '[taoensso.timbre :as log])
    (require '[dragonfiles.util :refer :all])
    (eval (read-string script))))


(defn script
  "Read and return the processor script from a file if the script starts
  with a @, otherwise it returns the script itself."
  [^String script]
  (when script
    (if (.startsWith script "@")
      (slurp (.substring script 1))
      script)))


(defn processor [script']
  (prepare-script (script script')))


(comment
  ;; REPL interaction

  (def file "/tmp/one.txt")

  (process-file file (eval (read-string "(comp s/upper-case slurp)")) "/tmp/one.out")


  )
