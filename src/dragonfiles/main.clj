(ns dragonfiles.main
  (:require [dragonfiles.core :as core])
  (:require [clojure.java.io :as io])
  (:require [clojure.tools.cli :refer [parse-opts]])
  (:require [dragonfiles.core-utils :refer :all])
  (:require [taoensso.timbre :as log])
  (:require [clojure.string :as s])
  (:gen-class))


(def cli-options

  [["-s" "--source PATH" "A file or directory containing text file to process"
    :validate [#(.exists (io/file %)) "The file or directory must exist"]]

   ["-o" "--output PATH" "A file or directory which will contains the output files."
    :validate [#(not (.exists (io/file %))) "The file or directory must NOT exist"]]

   ["-x" "--extension EXT" "Use the given extension for output files"]

   ["-f" "--file-mode" "Rather than processing line-by-line the function expects a file-in file-out"]

   ["-p" "--parallel" "Process files in parallel"]

   ["-i" "--init-script SCRIPT" "a script which is executed before the first file is processed."]

   ["-e" "--end-script SCRIPT" "a script which is executed after the last file is processed"]

   ["-m" "--module-script SCRIPT" "A script with function definitions to load. (repeatable)"
        :default []
        :assoc-fn (fn [m k v] (update-in m [k] conj v))
   ]

   ["-q" "--quiet" "Less verbose output"]

   ["-v" "--version" "Just print the version"]

   ["-h" "--help" "This help"]])


(defn version []
  (try
    (->> "project.clj"
         io/resource
         slurp
         read-string
         nnext
         first
         (str "v"))
    (catch Exception x)))


(defn logo []
  (try
    (->> "dragonfly60.txt"
         io/resource
         slurp)
    (catch Exception x)))


(defn head
  "return the logo with the version"
  []
  (format (logo) (version)))


(defn help!
  ([cli error exit]
   (help! (update-in cli [:errors] conj error) exit))
  ([{:keys [options arguments errors summary] :as cli} exit]
   (display (head))
   (display "SYNOPSIS")
   (display "       dgf -s PATH -o PATH   SCRIPT \n")
   (display summary)
   (display "")
   (doseq [error errors]
     (display error))
   (display "")
   (System/exit exit)))



(defn- expression-or-file
  "if the give script is not an expression but is a file,
  denoted by the starting `@`, then load the file instead."
  [^String script]
  (when script
    (let [is-file? (.startsWith (s/trim script) "@")
          file (when is-file? (.substring (.trim script) 1))
          file-expr (when file (s/trim (slurp file)))]
      (or file-expr script))))



(defn main* [script &
             {:keys [source output parallel file-mode init-script end-script
                     extension module-script]}]
  (let [;; the first task is to load the modules if present
        _  (doall (map (comp core/processor expression-or-file) module-script))
        ;; after modules have been initialised, the next step is to load
        ;; the init script
        _ (core/processor (expression-or-file init-script))
        ;; then we initialise the processor function
        processor (core/processor (expression-or-file script))
        ;; and finally the termination script
        end-script  (fn [] (core/processor (expression-or-file end-script)))]


    ;; Process all files
    (core/process-files processor source output
                        :parallel? parallel
                        :file-mode? file-mode
                        :extension extension)

    ;; run termination script
    (end-script)))


(defn -main [& args]
  (let [{:keys [options arguments errors summary] :as cli}
        (parse-opts args cli-options)]
    (cond
     (:version options)         (display (head))
     (:help options)            (help! cli 1)
     errors                     (help! cli 2)
     (nil? (:source options))   (help! cli "MISSING: required param '-s PATH'" 3)
     (nil? (:output options))   (help! cli "MISSING: required param '-o PATH'" 4)
     (not= 1 (count arguments)) (help! cli "ERROR: too many arguments" 5)



     :default
     (do
       (if (:quiet options)
         (init-log! :level :warn)
         (do
           (init-log! :level :debug)
           (display (head))))

       (log/trace "OPTIONS:" options)

       (apply main* (first arguments) (mapcat identity options))

       (log/trace "Shutting down!!!")
       (shutdown-agents)
       (System/exit 0)))))
