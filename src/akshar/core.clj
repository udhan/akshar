(ns akshar.core
  (:import (javax.swing JFrame JTextPane Action
                        KeyStroke AbstractAction
                        JScrollPane JPanel)
           (javax.swing.text DocumentFilter)
           (java.awt.event KeyListener KeyEvent)
           (java.awt BorderLayout Dimension)
           (java.io FileReader FileWriter))
  (:require [clojure.java.io :as io])
  (:gen-class))

;; State

(def state (atom {:mode :cmd :chord ""}))

(defn add-chord-key! [key]
  (swap! state update-in [:chord] (partial str key)))

(defn get-chord []
  (keyword (get @state :chord "not-found")))

(defn set-mode! [mode]
  (swap! state assoc :chord "")
  (swap! state assoc :mode mode))

(defn command-mode []
  (set-mode! :cmd))

(defn cmd? []
  (= (:mode @state) :cmd))

(defn insert-mode []
  (set-mode! :ins))

;; File operations

(defn open [file window]
  (.read (:b window) (FileReader. (.getAbsolutePath file)) nil))

(defn save! [file window]
  (.write (:b window) (FileWriter. file)))

;; Text pane related operations

(defn get-doc [text-pane]
  (.getStyledDocument text-pane))

(defn append-text [text doc]
  (.insertString doc (.getLength doc) (str "\n" text) nil))

(defn get-line-text [event]
  (let [offset (:offset event)
        doc (:doc event)
        ele (.getCharacterElement doc offset)
        soff (.getStartOffset ele)
        eoff (.getEndOffset ele)]
    (.getText doc soff (- eoff soff))))

(defn eval-line [event]
  (append-text 
   (with-out-str
     (try
       (load-string
        (get-line-text event))
       (catch Throwable t
         (println (map str (.getStackTrace t))))))
   (:doc event)))

;; Command mode chords

(def chords (atom {:i insert-mode
                   :e eval-line
                   :w save!}))

(defn handle-chords [event]
  (add-chord-key! (:ch event))
  (if-let [f (get @chords (get-chord))]
    (f event)))

;; Actions

(def esc-action
  (proxy [AbstractAction] []
    (actionPerformed [ae]
      (set-mode! :cmd))))

(def doc-filter
  (proxy [DocumentFilter] []
    (insertString [fb offset string attr]
      (if (cmd?)
        (handle-chords {:doc (.getDocument fb) :ch string :offset offset})
        (proxy-super insertString fb offset string attr)))

    (replace [fb offset length text attr]
      (if (cmd?)
        (handle-chords {:doc (.getDocument fb) :ch text :offset offset})
        (proxy-super replace fb offset length text attr)))))

;; UI elements

(defn add-keybindings [frame]
  (let [imap (.getInputMap (.getContentPane frame))]
    (.put imap (KeyStroke/getKeyStroke KeyEvent/VK_ESCAPE 0) esc-action)))

(defn make-text-pane []
   (JTextPane.))

(defn make-window []
  (let [panel (JPanel.)
        tag (make-text-pane)
        body (make-text-pane)]
    (.setPreferredSize tag (Dimension. 100 100))
    (.setLayout panel (BorderLayout.))
    (.add panel (JScrollPane. tag) BorderLayout/PAGE_START)
    (.add panel (JScrollPane. body) BorderLayout/CENTER)
    {:p panel :t tag :b body}))
  
(defn make-frame []
  (let [frame (JFrame. "Akshar")] 
    (add-keybindings frame)
    (doto frame
      (.setSize 200 200)
      (.setDefaultCloseOperation JFrame/EXIT_ON_CLOSE))))

(defn make-editor [fpath]
  (let [frame (make-frame)
        window (make-window)
        file (io/as-file fpath)
        res (open file window)]
    (.add (.getContentPane frame) (:p window) BorderLayout/CENTER)
    (.setDocumentFilter (.getDocument (:b window)) doc-filter)
    (.pack frame)
    {:frame frame
     :windows [window]}))

  

(defn start [fpath]
  (let [editor (make-editor fpath)]
  (.setVisible (:frame editor) true)
  (swap! state merge editor)
  (insert-mode)))
    
(defn -main
  [fpath & args]
  (start fpath))
