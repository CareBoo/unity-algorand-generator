(ns algoapi-gen.types
  (:require [clojure.string :as str]))

(defn clean-description
  [description]
  (if description
    (str/replace description #"\\" "")
    description))

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
      #{"sig" "msig" "lsig"} "SignatureType"
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
     :description (-> schema (:description) (clean-description))}))

(defn parse-type-def
  [[key {:keys [properties description]}]]
  (cond
    properties {:name (name key)
                :properties (map parse-property properties)
                :description (clean-description description)}
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

(defn get-property-equals
  [{:keys [type property-name]}]
  (-> (cond
        (= type "string") "StringComparer.Equals({property-name}, other.{property-name})"
        (str/ends-with? type "[]") "ArrayComparer.Equals({property-name}, other.{property-name})"
        :else "{property-name}.Equals(other.{property-name})")
      (str/replace #"\{property-name\}" property-name)))

(defn with-equals
  [property]
  (assoc property :equals (get-property-equals property)))

(defn with-property-info
  [modify-property type-def]
  (assoc type-def
          :properties (map modify-property (:properties type-def))))

(defn with-last-info
  [type-def]
  (assoc type-def
         :properties 
         (concat
          (butlast (:properties type-def))
          [(assoc (last (:properties type-def)) :last true)])))

(defn with-supplemental-info
  [type-defs]
  (->> type-defs
      (map (partial with-property-info with-equals))
      (map with-last-info)))

(defn get-type-defs
  [oapi]
  (-> oapi
      (:components)
      (#(into [] 
              [(get-type-defs-responses (:responses %))
               (get-type-defs-schemas (:schemas %))]))
      (flatten)
      (with-supplemental-info)))

(defn get-wrapper-types-schema
  [[key schema]]
  (let [{type :type} schema]
    (case type
      "array" {:name (name key)
               :wrapped-type (str (read-ref (:$ref (:items schema))) "[]")
               :description (-> schema (:description) (clean-description))
               :equals "ArrayComparer.Equals(WrappedValue, other.WrappedValue)"}
      nil)))

(defn get-wrapper-types
  [oapi]
  (->> oapi
       (:components)
       (:schemas)
       (keep get-wrapper-types-schema)))

(defn get-types
  [daemon oapi]
  {:name (-> daemon (name) (str/capitalize))
   :types (get-type-defs oapi)
   :wrapper-types (get-wrapper-types oapi)})
