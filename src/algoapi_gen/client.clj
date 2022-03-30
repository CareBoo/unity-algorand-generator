(ns algoapi-gen.client 
  (:require [clojure.string :as str]))

(defn get-methods
  [oapi]
  [])

(defn get-client
  [daemon oapi]
  {:name (-> daemon (name) (str/capitalize))
   :methods (get-methods oapi)})
