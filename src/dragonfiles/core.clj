(ns dragonfiles.core
  (:require [clojure.java.io :as io])
  (:require [clojure.string :refer [trim] :as s ])
  (:require [taoensso.timbre :as log])
  (:require [clojure.tools.cli :refer [parse-opts]])
  (:require [me.raynes.fs :as fs])
  (:require [dragonfiles.core-utils :refer :all])
  (:require [dragonfiles.util :refer [safely]])
  (:gen-class))


(defn ensure-dirs [file]
  (fs/mkdirs (.getParentFile (io/file file))))

(defn process-file [source processor output]
  (log/debug "Processing: " source " -> " output)

  (safely (str "Processing: " source " -> " output)
    (ensure-dirs output)
    ;; process the file
    (->> source
         processor
         (spit output))))



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

  (process-file file (processor "(comp frequencies #(s/split % #\" \") s/upper-case slurp)") "/tmp/one.out")


  )
