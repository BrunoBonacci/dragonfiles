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

(defn process-file [processor]
  (fn [source output]
    (log/debug "Processing: " source " -> " output)

    (safely (str "Processing: " source " -> " output)
      (ensure-dirs output)
      ;; process the file
      (processor source output))))


(defn line-processor [processor]

  (fn [source output]
    (with-open [rdr  (io/reader source)
                wrtr (io/writer output)]

      (doseq [line (line-seq rdr)]
        (.write wrtr
                (-> line
                    processor
                    (str \newline)))))))



(defn process-files [processor source output & {:keys [parallel? file-mode?]
                                                :or {prallel?   false
                                                     file-mode? false}}]
  (let [files  (build-files-list source output "txt")
        mapper (if parallel? #'pmap #'map)
        prox   (if file-mode?
                 (process-file processor)
                 (process-file (line-processor processor)))]
    (doall
     (mapper (fn [[src out]] (prox src out)) files))))


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

  (process-files
   (processor "(comp (partial s/join \"\\n\") #(s/split % #\"\\W+\") s/upper-case)")
   "/tmp/one.txt"
   "/tmp/one.out")

  (process-files
   (comp (partial s/join "\n") #(s/split % #"\W+") s/upper-case)
   "/tmp/one.txt"
   "/tmp/one.out")

  (process-files
   (processor "(fn [src out] (->> src slurp ((comp (partial s/join \"\\n\") #(s/split % #\"\\W+\") s/upper-case)) (spit out)))")
   "/tmp/one.txt"
   "/tmp/one.out"
   :file-mode? true)
  )
