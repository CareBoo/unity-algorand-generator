(ns algoapi-gen.core
  (:gen-class)
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [algoapi-gen.types :as types]
            [clostache.parser :as mustache]))

(def oapi-urls
  {:algod "https://raw.githubusercontent.com/algorand/go-algorand/master/daemon/algod/api/algod.oas3.yml"
   :indexer "https://raw.githubusercontent.com/algorand/indexer/develop/api/indexer.oas3.yml"})



(def mustache-partials
  (->> ["header"]
       (map (fn [x] [(keyword x)
                     (slurp (str "templates/" x ".mustache"))]))
       (into {})))

(defn doc-prefix
  [text]
  (fn [render]
    (let [rendered (render text)
          indent (-> text (str/split #"///") (first))]
      (->> rendered
           (str/split-lines)
           (str/join (str "\n" indent "/// "))
           ))))

(def mustache-lambdas
  {:doc-prefix-lambda doc-prefix})

(defn render-mustache
  [data template]
  (mustache/render 
   template
   (merge data mustache-lambdas) 
   mustache-partials))

(defn read-json
  [url]
  (json/read-str (slurp url)
                 :key-fn keyword))

(defn read-oapi
  [daemon]
  (read-json (daemon oapi-urls)))

(defn output-filename
  [daemon datatype]
  (str
   "dist/"
   (-> daemon (name) (str/capitalize))
   "/"
   (-> datatype (name) (str/capitalize))
   ".gen.cs"))

(defn template-for
  [datatype]
  (case datatype
   :types (slurp "templates/types.mustache")))

(defn output-mustache
  [data daemon datatype]
  (let [filename (output-filename daemon datatype)]
    (io/make-parents filename)
    (spit filename
        (render-mustache data (template-for datatype)))))

(defn output
  [daemon]
  (let [oapi (read-oapi daemon)]
    (-> daemon
        (types/get-types oapi)
        (output-mustache daemon :types))))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (output :algod)
  (output :indexer))
