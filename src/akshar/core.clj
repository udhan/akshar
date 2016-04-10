(ns akshar.core
  (:import (javax.swing JFrame JTextPane Action
                        KeyStroke AbstractAction
                        JScrollPane)
           (javax.swing.text DocumentFilter)
           (java.awt.event KeyListener KeyEvent)
           (java.awt BorderLayout Dimension))
  (:gen-class))

;; State
(def state (atom {:mode :cmd :chord ""}))

(defn set-mode! [mode]
  (println "mode" mode)
  (swap! state assoc :chord "")
  (swap! state assoc :mode mode))

(defn command []
  (set-mode! :cmd))

(defn insert []
  (set-mode! :ins))

(defn get-para [offset]
  (let [doc (:doc @state)
        ele (.getCharacterElement doc offset)
        soff (.getStartOffset ele)
        eoff (.getEndOffset ele)]
    (.getText doc soff (- eoff soff))))

(defn append-mb [string]
  (let [doc (:mbdoc @state)]
    (println doc)
    (.insertString doc (.getLength doc) (str "\n" string) nil)))

(defn eval-line []
  (append-mb 
   (with-out-str
     (try
       (load-string
        (get-para (.getCaretPosition (:tp @state))))
       (catch Throwable t
         (println (map str (.getStackTrace t))))))))

(def chords (atom {:i insert
                   :e eval-line}))

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
        mb (make-text-pane)
        mbdoc (.getStyledDocument mb)
        tp (make-text-pane)
        cp (.getContentPane frame)
        doc (.getStyledDocument tp)]
    (.setDocumentFilter doc doc-filter)
    (.setPreferredSize mb (Dimension. 300 200))
    (.add cp (JScrollPane. mb) (BorderLayout/PAGE_START))
    (.add cp (JScrollPane. tp) (BorderLayout/CENTER))
    (.pack frame)
    (add-keybindings tp)
    {:frame frame :tp tp :mb mb :mbdoc mbdoc :cp cp :doc doc}
    ))

(defn start []
  (swap! state merge (make-editor))
  (.setVisible (:frame @state) true)
  (insert))
    
(defn -main
  [& args]
  (start))
