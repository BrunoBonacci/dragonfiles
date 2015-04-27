(ns dragonfiles.main
  (:require [dragonfiles.core :as core])
  (:require [clojure.java.io :as io])
  (:require [clojure.tools.cli :refer [parse-opts]])
  (:require [dragonfiles.core-utils :refer :all])
  (:gen-class))


(def cli-options

  [["-s" "--source PATH" "A file or directory containing text file to process"
    :validate [#(.exists (io/file %)) "The file or directory must exist"]]

   ["-o" "--output PATH" "A file or directory which will contains the output files."
    :validate [#(not (.exists (io/file %))) "The file or directory must NOT exist"]]

   ["-i" "--init-script SCRIPT" "a function which is executed before the first file is processed"]

   ["-e" "--end-script SCRIPT" "a function which is executed after the last file is processed, and before the termination."]

   ["-f" "--file-mode" "Rather then processing line-by-line the function expects a file-in file-out"]

   ["-p" "--parallel" "Process files in parallel"]

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
   (doseq [error errors]
     (display error))
   (display summary)
   (System/exit exit)))


(defn- wrap-script [script]
  (str "(fn [] " script ")"))

(defn main* [script &
             {:keys [source output parallel file-mode init-script end-script]}]
  (let [processor (core/processor script)
        init-script (or (core/processor (wrap-script init-script)) (fn []))
        end-script  (or (core/processor (wrap-script end-script))  (fn []))]

    ;; initialize process
    (init-script)

    (core/process-files processor source output
                        :parallel? parallel
                        :file-mode? file-mode)

    ;; terminating process
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
       (apply main* (first arguments) options)
       (shutdown-agents)
       (System/exit 0)))))
