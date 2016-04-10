(ns akshar.core
  (:import (javax.swing JFrame JTextPane Action KeyStroke AbstractAction)
           (javax.swing.text DocumentFilter)
           (java.awt.event KeyListener KeyEvent))
  (:gen-class))

;; State
(def state (atom {:mode :cmd :chord ""}))

(defn set-mode! [mode]
  (swap! state assoc :chord "")
  (swap! state assoc :mode mode))

(def chords (atom {:i #(set-mode! :ins)}))

(defn cmd? []
  (= (:mode @state) :cmd))

(defn add-chord-key! [key]
  (swap! state update-in [:chord] (partial str key)))

(defn get-chord []
  (keyword (get @state :chord "not-found")))

(defn handle-chords [ch]
  (add-chord-key! ch)
  (if-let [f (get @chords (get-chord))]
    (f)))

;; Actions
(def esc-action
  (proxy [AbstractAction] []
    (actionPerformed [ae]
      (set-mode! :cmd))))

(def doc-filter
  (proxy [DocumentFilter] []
    (insertString [fb offset string attr]
      (if (cmd?)
        (handle-chords string)
        (proxy-super insertString fb offset string attr)))

    (replace [fb offset length text attr]
      (if (cmd?)
        (handle-chords text)
        (proxy-super replace fb offset length text attr)))))

;; UI elements
(defn make-text-pane []
   (JTextPane.))

(defn add-keybindings [tp]
  (let [im (.getInputMap tp)]
    (.put im (KeyStroke/getKeyStroke KeyEvent/VK_ESCAPE 0) esc-action)
    ))

(defn make-frame []
  (doto (JFrame. "Akshar")
    (.setSize 200 200)
    (.setDefaultCloseOperation JFrame/EXIT_ON_CLOSE)))

(defn make-editor []
  (let [frame (make-frame)
        tp (make-text-pane)
        cp (.getContentPane frame)
        doc (.getStyledDocument tp)]
    (.setDocumentFilter doc doc-filter)
    (.add cp tp)
    (add-keybindings tp)
    {:frame frame :tp tp :cp cp :doc doc}
    ))

(defn start []
  (swap! state merge (make-editor))
  (.setVisible (:frame @state) true))
    
(defn -main
  [& args]
  (start))
