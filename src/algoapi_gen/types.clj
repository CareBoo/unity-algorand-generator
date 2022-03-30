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
  [{:keys [enum format x-go-name x-algorand-format]}]
  (cond
    enum (get-enum-type enum)
    format (case format
             "byte" "byte[]"
             "json" "AlgoApiObject")
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

(defn get-type-defs-schema
  [[key {:keys [properties description]}]]
  (cond
    properties {:name (name key)
                :properties (map parse-property properties)
                :description (clean-description description)}
    :else nil))

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

(defn get-wrapper-types-schema
  [[key schema]]
  (let [ref (case (:type schema)
              "array" (-> schema (:items) (:$ref) (str "[]"))
              (:$ref schema))]
    (if ref
      {:name (name key)
       :wrapped-type (read-ref ref)
       :description (-> schema (:description) (clean-description))
       :equals "ArrayComparer.Equals(WrappedValue, other.WrappedValue)"}
      nil)))

(defn get-type-defs
  [schemas]
  (->> schemas
      (keep get-type-defs-schema)
      (with-supplemental-info)))
  
(defn get-wrapper-type-defs
  [schemas]
  (keep get-wrapper-types-schema schemas))

(defn format-response-as-schema
  [[key response]]
  (let [{schema :schema} response]
   [key
   (-> response
       (dissoc :schema)
       (merge schema))]))

(defn parse-oapi-for-schemas
  [oapi]
  (concat
   (->> oapi (:definitions))
   (->> oapi
        (:responses)
        (map format-response-as-schema))))

(defn get-types
  [daemon oapi]
  (let [schemas (parse-oapi-for-schemas oapi)]
   {:name (-> daemon (name) (str/capitalize))
   :types (get-type-defs schemas)
   :wrapper-types (get-wrapper-type-defs schemas)}))
