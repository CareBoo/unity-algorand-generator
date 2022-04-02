(ns algoapi-gen.client 
  (:require [algoapi-gen.types :as types]
            [clojure.string :as str]))

(defn false-if-empty
  [s]
  (if (empty? s)
    false
    s))

(defn with-last
  [last-name s]
  (concat
   (butlast s)
   [(assoc (last s) last-name true)]))

(defn get-method-parameter-ref
  [oapi ref]
  (let [path (-> ref (str/split #"/") (rest))]
    (loop [param oapi
           remaining path]
      (if (= (count remaining) 1) 
        (let [param-name (first remaining)]
          (assoc ((keyword param-name) param) :name param-name))
        (recur ((keyword (first remaining)) param)
               (rest remaining))))))

(defn get-method-parameter
  [oapi parameter]
  (let [{required? :required} parameter
        {ref :$ref} parameter
        {schema :schema, :or {schema parameter}} parameter]
    (if ref
      (->> ref
           (get-method-parameter-ref oapi)
           (get-method-parameter oapi))
      {:name (:name parameter)
       :field-name (types/camel-case (:name parameter))
       :type (types/get-type-ref schema required?)
       :description (:description parameter)
       :default? (not required?)
       :in (:in parameter)})))

(defn get-method-body-type
  [consumes]
  (let [consumes (set consumes)]
    (cond
      (contains? consumes "application/msgpack") "MessagePack"
      (contains? consumes "application/x-binary") "MessagePack"
      (contains? consumes "text/plain") "PlainText"
      :else "Json")))

(defn get-method-query
  [oapi {parameters :parameters}]
  (let [params (->> parameters
                (map (partial get-method-parameter oapi))
                (filter (fn [{in :in}] (= in "query"))))]
    (if (empty? params)
      false
      {:params params})))

(defn get-method-name
  [{method-name :operationId}]
  (let [[first & tail] method-name]
    (str (str/upper-case first) (str/join tail))))

(defn get-method-response
  [{{response :200} :responses}]
  (let [{schema :schema} response
        {ref :$ref} response]
    (cond
      schema {:response-description (:description response)
              :type (let [type (types/get-type-ref schema true)]
                      (if (= type "string")
                        "AlgoApiObject"
                        type))}
      ref {:type (types/get-type-ref response true)}
      :else nil)))

(defn get-method-summary
  [method]
  (:summary method))

(defn get-method-description
  [method]
  (:description method))

(defn get-method-parameters
  [oapi {parameters :parameters} path]
  (let [all-params (concat parameters (-> oapi (:paths) (path) (:parameters)))]
    (if all-params
      (->> all-params
           (map (partial get-method-parameter oapi))
           (group-by :name)
           (vals)
           (map first)
           (filter #(:name %))
           (sort-by :default?)
           (with-last :last-param))
      nil)))

(defn get-method-action
  [{action :action}]
  (str/capitalize (name action)))

(defn get-method-body
  [oapi method]
  (let [{consumes :consumes} method
        body-param (->> method
                        (:parameters)
                        (filter (fn [{in :in}] (= in "body")))
                        (map (partial get-method-parameter oapi))
                        (first))]
    (if body-param
      (assoc body-param :type (get-method-body-type consumes))
      false)))

(defn get-method-path
  [path]
  (-> path
      (name)
      ((partial str "/"))
      (str/replace
       #"\{([\w-]+)\}"
       #(str "{" (types/camel-case (%1 1)) "}"))))

(defn get-method-api
  [oapi [[path method]]]
  {:name (get-method-name method)
   :path (get-method-path path)
   :parameters (false-if-empty (get-method-parameters oapi method path))
   :summary (get-method-summary method)
   :description (get-method-description method)
   :response (get-method-response method)
   :query (get-method-query oapi method)
   :action (get-method-action method)
   :body (get-method-body oapi method)})

(defn get-actions-per-path
  [[path actions]]
  (->> actions
      (filter (fn [[action _]] 
                (contains? #{:get :post :put :delete} action)))
       (map (fn [[action method]]
              [path (assoc method :action action)]))))

(defn get-client-api
  [oapi]
  (->> oapi
       (:paths)
       (map get-actions-per-path)
       (keep (partial get-method-api oapi))
       (with-last :last-method)
       ))

(defn get-client
  [daemon oapi]
  {:name (-> daemon (name) (str/capitalize))
   :methods (get-client-api oapi)})
