(ns algoapi-gen.core
  (:gen-class)
  (:require [clojure.data.json :as json]))

(defn oapi-url
  [daemon]
  (case daemon
    :algod "https://raw.githubusercontent.com/algorand/go-algorand/master/daemon/algod/api/algod.oas3.yml"
    :indexer "https://raw.githubusercontent.com/algorand/indexer/develop/api/indexer.oas3.yml"))

(defn read-json
  [url]
  (json/read-str (slurp url)
                 :key-fn keyword))

(defn read-oapi
  [daemon]
  (read-json (oapi-url daemon)))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
