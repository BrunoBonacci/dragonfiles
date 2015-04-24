(ns dragonfiles.core-utils
  (:require [clojure.string :as s])
  (:require [clojure.java.io :as io])
  (:require [taoensso.timbre :as log])
  (:require [me.raynes.fs :as fs]))


(defn init-log!
  "Log initializaiton"
  []
  (log/set-config!
   [:fmt-output-fn]
   (fn [{:keys [level throwable message timestamp hostname ns]}
       & [{:keys [nofonts?] :as appender-fmt-output-opts}]]
     (format "[%s] - %s%s"
             (-> level name s/upper-case) (or message "")
             (or (log/stacktrace throwable "\n" (when nofonts? {})) ""))))

  (log/set-config! [:standard-out :fn]
                   (fn [{:keys [error? output]}]
                     (binding [*out* *err*]
                       (log/str-println output)))))



(defmacro display [& message]
  `(binding [*out* *out*]
    (println ~@message)))



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
