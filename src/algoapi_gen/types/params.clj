(ns algoapi-gen.types.params)

(def address
  {:matches [{:x-go-name "AccountId"}
             {:schema {:type "string"
                       :x-go-name "AccountId"}}
             {:name "address"}]
   :algosdk-type "Address"})

(def address-role
  {:matches [{:schema {:type "string"
                       :enum ["sender"
                              "receiver"
                              "freeze-target"]}}]
   :algosdk-type "AddressRole"})

(def date-time
  {:matches [{:schema {:x-algorand-format "RFC3339 String"}}
             {:x-algorand-format "RFC3339 String"}]
   :algosdk-type "DateTime"})

(def asset-id
  {:matches [{:schema {:x-go-name "AssetID"}}
             {:x-go-name "AssetID"}]
   :algosdk-type "AssetIndex"})

(def catchpoint
  {:matches [{:schema {:x-algorand-format "Catchpoint String"}}
             {:x-algorand-format "Catchpoint String"}]
   :algosdk-type "Catchpoint"})

(def required-uint64
  {:matches [{:required true
              :schema {:type "integer"}}]
   :algosdk-type "ulong"})

(def uint64
  {:matches [{:schema {:type "integer"}}]
   :algosdk-type "Optional<ulong>"})

(def bool
  {:matches [{:schema {:type "boolean"}}]
   :algosdk-type "Optional<bool>"})

(def response-format
  {:matches [{:schema {:type "string"
                       :enum ["json"
                              "msgpack"]}}]
   :algosdk-type "ResponseFormat"})

(def next-token
  {:matches [{:name "next"}]
   :algosdk-type "FixedString128Bytes"})

(def base64
  {:matches [{:schema {:x-algorand-format "base64"}}
             {:x-algorand-format "base64"}]
   :algosdk-type "string"
   :fmt "base64"})

(def sig-type
  {:matches [{:schema {:type "string"
                       :enum ["sig" "msig" "lsig"]}}]
   :algosdk-type "SignatureType"})

(def txid
  {:matches [{:x-go-name "TxID"
              :schema {:x-go-name "TxID"}}]
   :algosdk-type "TransactionId"})

(def tx-type
  {:matches [{:schema {:type "string"
                       :enum ["pay"
                              "keyreg"
                              "acfg"
                              "axfer"
                              "afrz"
                              "appl"]}}]
   :algosdk-type "TransactionType"})

