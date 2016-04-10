(ns akshar.core
  (:import (javax.swing JFrame JTextPane))
  (:gen-class))

(defn make-text-pane []
   (JTextPane.))

(defn make-frame []
  (doto (JFrame. "Akshar")
    (.setSize 200 200)))

(defn make-editor []
  (let [frame (make-frame)
        tp (make-text-pane)
        cp (.getContentPane frame)]
    (.add cp tp)
    {:frame frame :tp tp :cp cp}
    ))

(defn start []
  (def editor (make-editor))
  (.setVisible (:frame editor) true))
    
(defn -main
  [& args]
  (start))
