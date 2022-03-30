(ns algoapi-gen.mustache
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clostache.parser :as mustache]))

(defn read-template
  [template]
  (slurp (str "templates/" template ".mustache")))

(defn doc-prefix
  [text]
  (fn [render]
    (let [rendered (render text)
          indent (-> text (str/split #"///") (first))]
      (->> rendered
           (str/split-lines)
           (str/join (str "\n" indent "/// "))
           ))))

(def partials
  (->> ["header"]
       (map (fn [template] 
              [(keyword template)
               (read-template template)]))
       (into {})))

(def lambdas
  {:doc-prefix-lambda doc-prefix})

(defn render
  [data template]
  (mustache/render template (merge data lambdas) partials))

(defn output
  [data template filename]
  (io/make-parents filename)
  (spit filename (render data template)))
