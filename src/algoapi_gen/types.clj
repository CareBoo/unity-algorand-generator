(ns algoapi-gen.types)

(defn list-types
  [oapi]
  (map (fn [{{schemas :schemas} :components}] schemas) oapi))
