(ns the-wire.slack
  (:require [http.async.client :as http]
            [clojure.data.json :as json]
            [environ.core :refer [env]))

(def webhook_url
  (env :webook-url)

(def worry-phrases
  [
   "_I am so worried about %s, something's been up with her but still don't know what... Do you think I am awful momma?_"
   "_I AM *CEREAL* ABOUT THIS!! What we gonna do? %s might be lost, damaged or malnourished..._"
   "_What if %s is DEAD??? WE GOTTA DO SOMETHIN' BOUT IT._"
   "_This has been keeping me up at night. I can't put up with this presssure anymore..._"
   "_Maybe %s is in the shower... that such shower would be taking *suspciously* long tho..._"
   ])

(defn no-status-payload
  [body]
  {
   :color "#FF0000"
   :title (format "%s MIGHT BE IN SERIOUS TROUBLE!! PROBLEMAS!!1!" (clojure.string/upper-case (get-in body [:profile :first_name])))
   :text (format (rand-nth worry-phrases) (get-in body [:profile :first_name]))
   :mrkdwn_in ["field" "text"]
   :thumb_url (get-in body [:profile :image_192])})

(defn status-payload
  [body]
  {
   :color (format "#%s" (:color body))
   :title (format "%s'S UPDATED STATUS!! MUY BUENAS NOTICIAS!!1!" (clojure.string/upper-case (get-in body [:profile :first_name])))
   :mrkdwn_in ["fields"]
   :thumb_url (get-in body [:profile :image_192])
   :fields [
            {:value (format "_%s_"  (get-in body [:profile :status_text])) :short true}
            {:value (get-in body [:profile :status_emoji]) :short true}]})


(defn build-attachment
  [body]
  (if (empty? (get-in body [:profile :status_text]))
     (no-status-payload body)
     (status-payload body)))

(defn build
  [body]
  { :attachments [(build-attachment body)] })

(defn post-message
  [body]
  (println "Sending message...")
  (with-open
    [client (http/create-client)]
    (let [response (http/POST client webhook_url :body (json/write-str (build body)) :headers {:content-type "application/json"})]
      (println (json/write-str (build body)))
      (http/await response)
      (println (http/string response)))))
