(ns json.core
   (:require  [clj-http.client :as http]
              [clojure.data.json :as json]
              [clojure.string :as str]
              [clojure.data.csv :as csv]
              [clojure.java.io :as io]))

 (def studies-fields ["NCTId"
                      "StudyFirstSubmitQCDate"
                      "ResultsFirstSubmitDate"
                      "DispFirstSubmitDate"
                      "LastUpdateSubmitDate"
                      "StudyFirstPostDate"
                      "ResultsFirstSubmitQCDate"
                      "ResultsFirstPostDate"
                      "DispFirstSubmitQCDate"
                      "LastUpdatePostDate"
                      "StartDate"
                      "StartDateType"])

 (defn construct-api-query [fields-to-query & pagelimit]
   (if (empty? pagelimit)
     (let [base-url "https://clinicaltrials.gov/api/v2/studies?fields="
           fields (str/join "|" fields-to-query)]
       (str base-url fields))
     (let [base-url "https://clinicaltrials.gov/api/v2/studies?fields="
           fields (str base-url (str/join "|" fields-to-query) "&pageSize=")
           pl (first pagelimit)]
       (str fields pl))))

 (def url (construct-api-query studies-fields 1))

 (def nct2 "https://clinicaltrials.gov/api/v2/studies/NCT03235479")

 (defn get-ctgov-data [url]
   (loop [current-url url
          all-res []
          next-token nil]
     (let [res (json/read-str (:body (http/get current-url {:cookie-policy :none :disable-cookies true :accept :json})) :key-fn keyword)
           token (get-in res [:nextPageToken])
           res-studies (get-in res :studies)]
       (if (seq token)
         (let [updated-results (concat all-res res-studies)
               next-token (get-in res [:nextPageToken])
               next-nct (get-in res [:protocolSection :identificationModule :nctId])]
           (println "Next token:" next-token)
           (println "Next NCT:" next-nct)
           (recur (str url "&pageToken=" next-token) updated-results next-token))
         all-res))))

 (defn get-ctgov-data-with-counter [url]
   (loop [current-url url
          all-res []
          next-token nil
          loop-count 0] ; Initialize a loop counter
     (if (< loop-count 3) ; Check if loop-count is less than 3
       (let [res (json/read-str (:body (http/get current-url {:cookie-policy :none :disable-cookies true :accept :json})) :key-fn keyword)
             token (get-in res [:nextPageToken])
             res-studies (get-in res [:studies])]
         (if (seq token)
           (let [updated-results (concat all-res res-studies)
                 next-token (get-in res [:nextPageToken])
                 next-nct (get-in res [:protocolSection :identificationModule :nctId])]
             (println "Next token:" next-token)
             (println "Next NCT:" next-nct)
             (recur (str url "&pageToken=" next-token) updated-results next-token (inc loop-count))) ; Increment loop-count
           all-res))
       all-res))) ; Return accumulated results after 3 loops

 (def data (get-ctgov-data-with-counter url))

 data
 (def res-body
   ((json/read-str
     (:body (http/get url {:cookie-policy :none :disable-cookies true :accept :json}))
     :key-fn keyword)
    :studies))
 res-body

 (def test-dat
   (json/read-str
    (:body (http/get nct2 {:cookie-policy :none :disable-cookies true :accept :json}))
    :key-fn keyword))

 (get-in test-dat [:protocolSection :statusModule :startDateStruct :type])

 (def studies
   (vec (map (fn [entry]
               (let [nct_id (get-in entry [:protocolSection :identificationModule :nctId])
                     study_first_submitted_date (get-in entry [:protocolSection :statusModule :studyFirstSubmitQcDate])
                     results_first_submitted_date (get-in entry [:protocolSection :statusModule :resultsFirstSubmitDate])
                     disp_first_submit_date (get-in entry [:protocolSection :statusModule :dispFirstSubmitDate])
                     last_update_submit_date (get-in entry [:protocolSection :statusModule :lastUpdateSubmitDate])
                     study_first_post_date (get-in entry [:protocolSection :statusModule :studyFirstPostDateStruct :date])
                     results_first_submitted_qc_date (get-in entry [:protocolSection :statusModule :resultsFirstSubmitQcDate])
                     results_first_post_date (get-in entry [:protocolSection :statusModule :resultsFirstPostDateStruct :date])
                     disp_first_submitted_qc_date (get-in entry [:protocolSection :statusModule :dispFirstSubmitDate])
                     last_update_posted_date (get-in entry [:protocolSection :statusModule :lastUpdatePostDateStruct :date])
                     start_date (get-in entry [:protocolSection :statusModule :startDateStruct :date])
                     start_date_type (get-in entry [:protocolSection :statusModule :startDateStruct :type])]
                 {:nct_id nct_id
                  :study_first_submitted_date study_first_submitted_date
                  :results_first_submitted_date results_first_submitted_date
                  :disp_first_submit_date disp_first_submit_date
                  :last_update_submit_date last_update_submit_date
                  :study_first_post_date study_first_post_date
                  :results_first_submitted_qc_date results_first_submitted_qc_date
                  :results_first_post_date results_first_post_date
                  :disp_first_submitted_qc_date disp_first_submitted_qc_date
                  :last_update_posted_date last_update_posted_date
                  :start_date start_date
                  :start_date_type start_date_type}))
             data)))

 studies
 (def custom-headers
   ["study_first_submitted_date"
    "results_first_post_date"])


 (def transformed-data
   (map (fn [entry]
          (map entry custom-headers))
        my-data))


 (with-open [writer (clojure.java.io/writer "asdf.csv")]
   (csv/write-csv writer
                  (cons  custom-headers; Replace with your actual header names
                         transformed-data))))


(def data [studies])
(first studies)

(def data
  ({:study_first_submitted_date "2021-08-18",
    :results_first_post_date nil,
    :results_first_submitted_date nil,
    :last_update_posted_date "2024-02-23",
    :results_first_submitted_qc_date nil,
    :start_date_type "ACTUAL",
    :start_date "2021-10-18",
    :disp_first_submitted_qc_date nil,
    :disp_first_submit_date nil,
    :nct_id "NCT05013879",
    :last_update_submit_date "2024-02-21",
    :study_first_post_date "2021-08-19"}
   {:study_first_submitted_date "2011-07-24",
    :results_first_post_date nil,
    :results_first_submitted_date nil,
    :last_update_posted_date "2011-08-08",
    :results_first_submitted_qc_date nil,
    :start_date_type nil,
    :start_date "2004-10",
    :disp_first_submitted_qc_date nil,
    :disp_first_submit_date nil,
    :nct_id "NCT01402479",
    :last_update_submit_date "2011-08-05",
    :study_first_post_date "2011-07-26"}))

(vec my-data)

(vec studies)

(conj custom-headers transformed-data)