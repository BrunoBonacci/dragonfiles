(ns dragonfiles.util
  (:require [taoensso.timbre :as log])
  (:require [cheshire.core :as json]))


(defn to-json
  "Convert a Clojure data structure into it's json pretty print equivalent
   or compact version.
   usage:

   (to-json {:a \"value\" :b 123})
   ;=> {
   ;=>   \"a\" : \"value\",
   ;=>   \"b\" : 123
   ;=> }

   (to-json {:a \"value\" :b 123} :compact true)
   ;=> {\"a\":\"value\",\"b\":123}
   "
  [data & {:keys [pretty] :or {pretty false}}]
  (if-not data
    ""
    (-> data
        (json/generate-string {:pretty pretty})
        ((fn [s] (if pretty (str s \newline) s))))))


(defn from-json
  "Convert a json string into a Clojure data structure
   with keyword as keys"
  [data]
  (if-not data
    nil
    (-> data
        (json/parse-string true))))



(defmacro safely [message & body]
  `(try
     ~@body
     (catch Exception x#
       (log/warn x# ~message))))
