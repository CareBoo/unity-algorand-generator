(ns algoapi-gen.core
  (:gen-class)
  (:require [algoapi-gen.client :as client]
            [algoapi-gen.mustache :as mustache]
            [algoapi-gen.types :as types]
            [clojure.data.json :as json]
            [clojure.string :as str]))

(def oapi-urls
  {:algod "https://raw.githubusercontent.com/algorand/go-algorand/master/daemon/algod/api/algod.oas2.json"
   :indexer "https://raw.githubusercontent.com/algorand/indexer/develop/api/indexer.oas2.json"})


(defn read-json
  [url]
  (json/read-str (slurp url)
                 :key-fn keyword))

(defn read-oapi
  [daemon]
  (read-json (daemon oapi-urls)))

(def templates
  {:types "templates/types.mustache"
   :client "templates/client.mustache"})

(defn output-path
  [daemon datatype]
  (let [dir (-> daemon (name) (str/capitalize))
        filename (-> datatype (name) (str/capitalize))]
    (str "dist/" dir "/" filename ".gen.cs")))

(defn output-types
  [daemon oapi]
  (let [template (slurp (:types templates))
        path (output-path daemon :types)]
    (-> daemon
        (types/get-types oapi)
        (mustache/output template path))))

(defn output-client
  [daemon oapi]
  (let [template (:client templates)
        path (output-path daemon :client)]
    (-> daemon
        (client/get-client oapi)
        (mustache/output template path))))

(defn output
  [daemon]
  (let [oapi (read-oapi daemon)]
    (output-types daemon oapi)))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (output :algod)
  (output :indexer))
