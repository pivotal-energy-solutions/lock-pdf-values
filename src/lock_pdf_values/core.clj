(ns lock-pdf-values.core
  (:require [clojure.string :as string]
            [clojure.tools.cli :refer [parse-opts]]
            [pdfboxing.common :as common]
            [clojure.pprint :refer :all])
  (:import
   [org.apache.pdfbox.pdmodel PDDocumentCatalog]
   [org.apache.pdfbox.pdmodel.common COSObjectable]
   [org.apache.pdfbox.pdmodel.interactive.form PDAcroForm PDField PDSignatureField]
   [java.io IOException])
  (:gen-class))

;; https://pdfbox.apache.org/docs/2.0.7/javadocs

(def read-only-bit 1)

(defn has-value? [field]
  (seq (.getValueAsString field)))

(defn get-fields [document]
  (->> document
       (.getDocumentCatalog)
       (.getAcroForm)
       (.getFields)))

(defn set-field-readonly [field]
  (.setReadOnly field true)
  field)

(defn print-field [field]
  (println (str "  - " (.getPartialName field))))

(defn lock-fields [document]
  (->> (get-fields document)
       (filter has-value?)
       (map set-field-readonly)
       (run! print-field)))

(defn usage [options-summary]
  (->> ["Provided a PDF file with interactive form in it."
        "Will crawl through and set any field with a value to readonly."
        ""
        "Usage: lock-pdf-values -f INPUT-FILE [-o OUTPUT-FILE]"
        ""
        "Options:"
        options-summary
        ""]
       (string/join \newline)))

(def cli-options [["-f" "--filename FILENAME" "File Location"]
                  ["-o" "--output FILENAME" "File Destination (Defaults to input location)"]
                  ["-h" "--help"]])



(defn main [input output]
  (println (str "Locking fields for document: " input))
  (with-open [document (common/obtain-document input)]
    (dorun (lock-fields document))
    (.save document output))
  (println (str "Output: " output)))

(defn -main [& args]
  (let [{:keys [options arguments summary errors]} (parse-opts args cli-options)]
    (when (:help options)
      (println (usage summary))
      (System/exit0))
    (let [filename (:filename options)
          output (or (:output options) filename)]
      (lock-fields filename output))
  (System/exit 0)))
