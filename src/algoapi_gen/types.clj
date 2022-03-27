(ns algoapi-gen.types
  (:require [clojure.string :as str]))

(defn apply-to-first
  [func s]
  (let [[head & tail] s]
    (str (func head) (str/join tail))))

(defn pascal-case
  [name]
  (-> name
      (str/split #"-")
      ((partial map (partial apply-to-first str/upper-case)))
      (str/join)))

(defn camel-case
  [name]
  (->> name
      (pascal-case)
      (apply-to-first str/lower-case)))

(defn read-ref
  [ref]
  (-> ref
      (str/split #"/")
      (last)))

(defn get-int-type
  [{:keys [x-go-name]}]
  (case x-go-name
    "AssetID" "AssetIndex"
    "ulong"))

(defn get-enum-type
  [enum]
  (let [e (set enum)]
    (case e
      #{"pay" "keyreg" "acfg" "axfer" "afrz" "appl"} "TransactionType"
      #{"json" "msgpack"} "ResponseFormat"
      #{"sig" "msig" "lsig"} "SigType"
      #{"sumhash" "sha512_256"} "HashType"
      #{"all" "none"} "ExcludeFields"
      #{"sender" "receiver" "freeze-target"} "AddressRole"
      "string")))

(defn get-string-type
  [{:keys [x-go-name x-algorand-format enum]}]
  (cond
    enum (get-enum-type enum)
    x-go-name (case x-go-name
                "AccountID" "Address"
                "TxID" "TransactionId")
    x-algorand-format (case x-algorand-format
                        "RFC3339 String" "DateTime"
                        "Catchpoint String" "string"
                        "base64" "byte[]"
                        "Address" "Address"
                        "TEALProgram" "CompiledTeal"
                        "SignedTransaction" "byte[]")
    :else "string"))

(defn get-object-type
  [{:keys [x-algorand-format]}]
  (case x-algorand-format
    "SignedTransaction" "SignedTxn"
    nil "AlgoApiObject"
    x-algorand-format))

(defn get-type-ref
  [schema]
  (let [{ref :$ref, type :type} schema]
    (if ref
      (read-ref ref)
      (case type
        "string" (get-string-type schema)
        "object" (get-object-type schema)
        "integer" (get-int-type schema)
        "array" (-> schema (:items) (get-type-ref) (str "[]"))
        "boolean" "bool"))))

(defn parse-property
  [[key schema]]
  (let [name (name key)]
    {:api-name name
     :field-name (camel-case name)
     :property-name (pascal-case name)
     :type (get-type-ref schema)
     :description (:description schema)}))

(defn parse-type-def
  [[key {:keys [properties description]}]]
  (cond
    properties {:name (name key)
                :properties (map parse-property properties)
                :description description}
    :else nil))

(defn format-response
  [[name {{{schema :schema} :application/json} :content}]]
  [name schema])

(defn get-type-defs-responses
  [responses]
  (keep 
   (fn [response]
       (-> response (format-response) (parse-type-def)))
   responses))

(defn get-type-defs-schemas
  [schemas]
  (keep
   parse-type-def
   schemas))

(defn with-string-info
  [property]
  (assoc property
         :is-string (= (:type property) "string")))

(defn with-array-info
  [property]
  (let [{property-type :type} property]
   (assoc property 
         :is-array (and
                    property-type
                    (str/ends-with? property-type "[]")))))

(defn with-property-info
  [modify-property type-def]
  (assoc type-def
          :properties (map modify-property (:properties type-def))))

(defn with-last-info
  [type-defs]
  (concat
   (butlast type-defs)
   [(assoc (last type-defs) :last true)]))

(defn with-supplemental-info
  [type-defs]
  (->> type-defs
      (map (partial with-property-info with-string-info))
      (map (partial with-property-info with-array-info))
      (with-last-info)))

(defn get-type-defs
  [oapi]
  (-> oapi
      (:components)
      (#(into [] 
              [(get-type-defs-responses (:responses %))
               (get-type-defs-schemas (:schemas %))]))
      (flatten)
      (with-supplemental-info)))

(defn get-types
  [daemon oapi]
  {:name (-> daemon (name) (str/capitalize))
   :types (get-type-defs oapi)})
