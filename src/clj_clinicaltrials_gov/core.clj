(ns clj-clinicaltrials-gov.core
   (:require  [clj-http.client :as http]
              [clojure.data.json :as json]
              [clojure.string :as str]
              [clojure.data.csv :as csv]
              [clojure.java.io :as io]
              [tablecloth.api :as tc]))

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
                      "StartDateType"
                      "StatusVerifiedDate"
                      "CompletionDate"
                      "CompletionDateType"])

 
 (defn construct-api-query [fields-to-query & pagelimit]
   (if (empty? pagelimit)
     (let [base-url "https://clinicaltrials.gov/api/v2/studies?fields="
           fields (str/join "|" fields-to-query)]
       (str base-url fields))
     (let [base-url "https://clinicaltrials.gov/api/v2/studies?fields="
           fields (str base-url (str/join "|" fields-to-query) "&pageSize=")
           pl (first pagelimit)]
       (str fields pl))))

(def url (construct-api-query studies-fields 800))

(defn get-ctgov-data-with-counter [url]
   (loop [current-url url
          all-res []
          next-token nil
          retries 0] ; Initialize a loop counter
     (if (< retries 6) ; Check if loop-count is less than 3
       (let [raw-res (http/get current-url {:cookie-policy :none :disable-cookies true :accept :json})
             res (json/read-str (:body raw-res) :key-fn keyword)
             token (:nextPageToken res)
             res-studies (:studies res)
             status (:status raw-res)]
         (println "Status is: " status)
         (println "Num of retries is:" retries)
         (if (= status 429)
           (do
             (println "Rate limited, retrying...")
             (Thread/sleep 1000)
             (recur current-url all-res next-token (inc retries)))
           (if (seq token)
             (let [updated-results (concat all-res res-studies)
                   next-token (get-in res [:nextPageToken])
                   num_fetch (count res-studies)]
               (println "Next token:" next-token)
               (println "Number of Studies Fetched:" num_fetch)
               (recur (str url "&pageToken=" next-token) updated-results next-token retries)) ; Increment loop-count
             all-res)))
       all-res))) ; Return accumulated results after 3 loops


(def studies
   (tc/dataset (vec (map (fn [entry]
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
                     start_date_type (get-in entry [:protocolSection :statusModule :startDateStruct :type])
                     status_verified_date (get-in entry [:protocolSection :statusModule :statusVerifiedDate])
                     completion_date (get-in entry [:protocolSection :statusModule :completionDateStruct :date])
                     completion_date_type (get-in entry [:protocolSection :statusModule :completionDateStruct :type])
                     
                     ]
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
                  :start_date_type start_date_type
                  :status_verified_date status_verified_date
                  :completion_date completion_date
                  :completion_date_type completion_date_type
                  
                  }))
             (get-ctgov-data-with-counter url)))))

(tc/write-csv! tc_dat "test.csv")
