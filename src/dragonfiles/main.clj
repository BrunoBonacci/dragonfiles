(ns dragonfiles.main
  (:require [dragonfiles.core :as core])
  (:require [clojure.java.io :as io])
  (:require [clojure.tools.cli :refer [parse-opts]])
  (:require [me.raynes.fs :as fs])
  (:gen-class))


(def cli-options

  [["-s" "--source PATH" "A file or directory containing text file to process"
    :validate [#(.exists (io/file %)) "The file or directory must exist"]]

   ["-o" "--output PATH" "A file or directory which will contains the output files."
    :validate [#(not (.exists (io/file %))) "The file or directory must NOT exist"]]

   ["-p" "--parallel" "Process files in parallel"]

   ["-v" "--version" "Just print version"]

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
   (println (head))
   (doseq [error errors]
     (println error))
   (println summary)
   (System/exit exit)))


(defn -main [& args]
  (let [{:keys [options arguments errors summary] :as cli}
        (parse-opts args cli-options)]
    (cond
     (:version options)         (println (head))
     (:help options)            (help! cli 1)
     errors                     (help! cli 2)
     (nil? (:source options))   (help! cli "MISSING: required param '-s PATH'" 3)
     (nil? (:output options))   (help! cli "MISSING: required param '-o PATH'" 4)
     (not= 1 (count arguments)) (help! cli "ERROR: too many arguments" 5)



     :default
     (let [{:keys [source output parallel]} options
           processor (core/processor (first arguments))]
       (core/process-files source processor output parallel)
       (shutdown-agents)))))
