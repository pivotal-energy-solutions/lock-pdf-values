(ns lock-pdf-values.core
  (:require [clojure.string :as string]
            [clojure.tools.cli :refer [parse-opts]]
            [pdfboxing.common :as common]
            [clojure.pprint :refer :all])
  (:import
   [org.apache.pdfbox.pdmodel PDDocumentCatalog]
   [org.apache.pdfbox.pdmodel.common COSObjectable]
   [org.apache.pdfbox.pdmodel.interactive.form PDAcroForm PDField PDSignatureField PDTextField PDCheckBox PDChoice]
   [java.io IOException])
  (:gen-class))

;; https://pdfbox.apache.org/docs/2.0.7/javadocs

(defn seq-contains? [coll target]
  ;(println target " in: " (some #(= target %) coll))
  (some #(= target %) coll)
  )

(defn get-fields [document]
  (->> document
       (.getDocumentCatalog)
       (.getAcroForm)
       (.getFields)))

(defn usage [options-summary]
  (->> ["Provided a PDF file with interactive form in it."
        "Will crawl through and set any field with a value to readonly."
        ""
        "Usage: lock-pdf-values -f INPUT-FILE [-o OUTPUT-FILE] -F \"<Field1>,Field 2>,<Field n>\""
        ""
        "Options:"
        options-summary
        ""]
       (string/join \newline)))

(def cli-options [["-f" "--filename FILENAME" "File Location"]
                  ["-o" "--output FILENAME" "File Destination (Defaults to input location)"]
                  ["-F" "--fields <field 1>,<field 2>,<field n>" "Fields to lock"]
                  ["-h" "--help"]])

(defn main [input output fields]
  (println (str "Locking fields for document: " input))
  (def field-names (string/split fields #","))
  (println (str "Locking field names: " field-names))
  (with-open [document (common/obtain-document input)]
    (def catalog (.getDocumentCatalog document))
    (def catalog_form (.getAcroForm catalog))

    (println (str "Default Appearance: " (.getDefaultAppearance catalog_form)))

    (doseq [field (get-fields document)]
      (if (seq-contains? field-names (.getPartialName field))
        (if (instance? PDCheckBox field)
          (if (.isChecked field)
            (do
              ;(println (str "Check box Checked: " (.getPartialName field)))
              (.setReadOnly field true)
            )
            (do
              ;(println (str "Check box Un-Checked: " (.getPartialName field)))
              (.setReadOnly field true)
            )
          )
          ; If it not a checkbox lock it!
          (do
            ;(println (str "Field: " (.getPartialName field)))
            (.setReadOnly field true)
          )
        )
      )
    )
    (.refreshAppearances catalog_form)
    (.save document output))
  (println (str "Output: " output)))

(defn -main [& args]
  (let [{:keys [options arguments summary errors]} (parse-opts args cli-options)]
    (when (:help options)
      (println (usage summary))
      (System/exit 0))
    (let [filename (:filename options)
          output (or (:output options) filename)
          fields (:fields options)]
      (main filename output fields))
    (System/exit 0)))
