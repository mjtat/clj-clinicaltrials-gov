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
                      "CompletionDateType"
                      "PrimaryCompletionDate"
                      "PrimaryCompletionDateType"
                      "TargetDuration"
                      "StudyType"
                      "Acronym"
                      "BaselinePopulationDescription"
                      "BriefTitle"
                      "OfficialTitle"
                      "OverallStatus"
                      "LastKnownStatus"])

 ;; constructs query
 (defn construct-api-query [fields-to-query & pagelimit]
   (if (empty? pagelimit) ;;if there is no pagelimit then omit the pagesize api parameter
     (let [base-url "https://clinicaltrials.gov/api/v2/studies?fields="
           fields (str/join "|" fields-to-query)]
       (str base-url fields))
     (let [base-url "https://clinicaltrials.gov/api/v2/studies?fields=" ;;if there is a pagelimit declared, at the pagesize api parameter
           fields (str base-url (str/join "|" fields-to-query) "&pageSize=")
           pl (first pagelimit)]
       (str fields pl))))

(def url (construct-api-query studies-fields 525))

(defn get-ctgov-data-with-counter [url]
   (loop [current-url url
          all-res []
          next-token nil
          retries 0] ;; set base parameters for recursive loop
     (if (< retries 6) ;; if get 429 error, retry
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
             (recur current-url all-res next-token (inc retries))) ;;retry if status 429
           (if (seq token) ;if a token exists recurse and get the next page of resuylts
             (let [updated-results (concat all-res res-studies) ;; append new results to old results
                   next-token (get-in res [:nextPageToken])
                   num_fetch (count res-studies)]
               (println "Next token:" next-token)
               (println "Number of Studies Fetched:" num_fetch)
               (recur (str url "&pageToken=" next-token) updated-results next-token retries)) ;; do recursion
             all-res)))
       all-res)))

;; below calls the api, gets the declared variables, saves them into a dataframe in tablecloth
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
                     primary_completion_date (get-in entry [:protocolSection :statusModule :primaryCompletionDateStruct :date])
                     primary_completion_date_type (get-in entry [:protocolSection :statusModule :primaryCompletionDateStruct :type])
                     target_duration (get-in entry [:protocolSection :designModule :targetDuration])
                     study_type (get-in entry [:protocolSection :designModule :studyType])
                     acronym (get-in entry [:protocolSection :identificationModule :acronym])
                     baseline_pop_description (get-in entry [:resultsSection :baselineCharacteristicsModule :populationDescription])
                     brief_title (get-in entry [:protocolSection :identificationModule :briefTitle])
                     official_title (get-in entry [:protocolSection :identificationModule :officialTitle])
                     overall_status (get-in entry [:protocolSection :statusModule :overallStatus])
                     last_known_status (get-in entry [:protocolSection :statusModule :lastKnownStatus])
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
                  :primary_completion_date primary_completion_date
                  :primary_completion_date_type primary_completion_date_type
                  :target_duration target_duration
                  :study_type study_type
                  :acronym acronym
                  :baseline_pop_description baseline_pop_description
                  :brief_title brief_title
                  :official_title official_title
                  :overall_status overall_status
                  :last_known_status last_known_status
                  }))
             (get-ctgov-data-with-counter url)))))


(tc/write-csv! tc_dat "test.csv")
