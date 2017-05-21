(ns the-wire.core
  (:require [http.async.client :as http]
            [http.async.client.websocket :as websocket]
            [clojure.data.json :as json]
            [the-wire.slack :refer [post-message]]
            [clojure.core.async :refer [go-loop]]
            [environ.core :refer [env]))

(defn on-open [ws]
  (println "Connected to WebSocket."))

(defn on-close [ws code reason]
  (println "Connection to WebSocket closed.\n"
           (format "[%s] %s" code reason)))

(defn on-error [ws e]
  (println "ERROR:" e))

(defn handle-message [ws msg]
  (let [body (json/read-str msg :key-fn keyword)]
    (if (= "user_change" (:type body))
      (post-message (:user body)))))

(def auth_url
  (format "https://slack.com/api/rtm.connect?token=%s" (env :token)))

(def ping-msg
  (json/write-str {:id (rand-int 2000) :type "ping"}))

(defn connect-ws
  [ws-url]
  (println "Connecting...")
  (with-open [client (http/create-client)]
    (let [ws (http/websocket client
                             ws-url
                             :open  on-open
                             :close on-close
                             :error on-error
                             :text handle-message)]
      (loop []
        (Thread/sleep (+ 2000 (rand-int 7000)))
        (websocket/send ws :text ping-msg)
        (recur)))))

(defn connection
  []
  (with-open [client (http/create-client)]
    (let [response (http/POST client auth_url)]
      (println "Auth...")
      (http/await response)
      (let [response-body (json/read-str (http/string response) :key-fn keyword)]
        (connect-ws (:url response-body))))))


(defn -main
  []
  (try
    (connection)
    (catch Exception e (do
                         (println "PUTA")
                         (connection)))))
