(ns dragonfiles.core-utils
  (:require [clojure.string :as s])
  (:require [clojure.java.io :as io])
  (:require [taoensso.timbre :as log])
  (:require [me.raynes.fs :as fs]))


(defn init-log!
  "Log initializaiton"
  [& {:keys [level] :as opts}]
  (log/set-config!
   [:fmt-output-fn]
   (fn [{:keys [level throwable message timestamp hostname ns]}
       & [{:keys [nofonts?] :or {nofonts? true} :as appender-fmt-output-opts}]]
     (format "[%s] - %s%s"
             (-> level name s/upper-case) (or message "")
             (or (log/stacktrace throwable "\n" (when nofonts? {})) ""))))

  (log/set-config! [:standard-out :fn]
                   (fn [{:keys [error? output]}]
                     (binding [*out* *err*]
                       (log/str-println output))))
  (when level
    (log/set-level! level)))



(defmacro display [& message]
  `(binding [*out* *out*]
    (println ~@message)))



(defn target-file-name [root file output ext]
  (let [^String root (.getCanonicalPath (io/file root))
        ^String file (.getCanonicalPath (io/file file))
        ^String output (.getCanonicalPath (io/file output))
        ext-fn (fn [^String file]
                 (if (or (= :same ext) (nil? ext))
                   file
                   (.replaceFirst file (str "\\.[^.]*$") (str "." ext))))]
    (-> file
        (.replaceFirst (str "^\\Q" root "\\E" ) output)
        ext-fn
        io/file)))


(defn build-files-list
  "Build a list of tuples where the first element is tha name
  of the file in input and the second name is the file in output.
  such as:

      [[ \"/tmp/input/file1.txt\" \"/tmp/output/file1.tsv\"] ,,,]

  "
  [source output output-ext]
  (let [file-list      (filter #(.isFile %) (fs/find-files source #".*"))
        single-file-mode (.isFile (io/file source))]
    (if single-file-mode
      [[(first file-list) (io/file output)]]
      (map (juxt identity #(target-file-name source % output output-ext)) file-list))))
