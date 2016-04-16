(ns akshar.core
  (:import (javax.swing JFrame JTextPane Action KeyStroke AbstractAction
                        JScrollPane JPanel JComponent)
           (javax.swing.text DocumentFilter)
           (java.awt.event KeyListener KeyEvent)
           (java.awt BorderLayout Dimension Font)
           (java.io FileReader FileWriter))
  (:require [clojure.java.io :as io]
            [me.raynes.fs :as fs]
            [clojure.string :as strn])
  (:gen-class))
;; File system

(defn get-cwd-list []
  (fs/list-dir fs/*cwd*))

;; State

(def state (atom {:wid 0 :mode :cmd :chord ""}))

(defn get-next-wid []
  (:wid (swap! state update-in [:wid] inc)))

(defn add-chord-key! [key]
  (swap! state update-in [:chord] (partial str key)))

(defn get-chord []
  (keyword (get @state :chord "not-found")))

(defn set-mode! [mode]
  (println "Mode:" mode)
  (swap! state assoc :chord "")
  (swap! state assoc :mode mode))

(defn command-mode []
  (set-mode! :cmd))

(defn cmd? []
  (= (:mode @state) :cmd))

(defn insert-mode [& args]
  (set-mode! :ins))

;; File operations

(defn refresh-tag-text [window]
  (let [name (.getAbsolutePath (:file window))
        save (if (:dirty window) "Save" "")
        sep " "]
    (.setText (:tag window) (str name sep save))))

(defn open [fpath win]
  (let [file (io/as-file fpath)
        window (assoc win :file file)]
    (if (.isFile file)
      (do
        (.read (:body window) (FileReader. (.getAbsolutePath (:file window))) nil))
      (let [ls (fs/list-dir fpath)]
        (.setText (:body window) (strn/join "\n" ls))))
    (refresh-tag-text window)
    window))

(defn save! [event]
  (println event)
  (refresh-tag-text (:window event))
  (.write (:body (:window event)) (FileWriter. (:file (:window event)))))

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

(defn find-window [doc]
  ((:windows @state) (.getProperty doc "wid")))

(defn handle-chords [event]
  (add-chord-key! (:ch event))
  (if-let [f (get @chords (get-chord))]
    (f event)))

;; Actions
(defn set-dirty [window flag]
  (swap! state assoc-in [:windows (:wid window) :dirty] true)
  (get-in @state [:windows (:wid window)]))

(def esc-action
  (proxy [AbstractAction] []
    (actionPerformed [ae]
                     (set-mode! :cmd))))

(def doc-filter
  (proxy [DocumentFilter] []
    (insertString [fb offset string attr]
                  (let [window (find-window (.getDocument fb))
                        event {:doc (.getDocument fb) :ch string :offset offset :window window}]
                    (if (cmd?)
                      (handle-chords event)
                      (do (proxy-super insertString fb offset string attr)
                        (refresh-tag-text (set-dirty window true))))))

    (remove [fb offset length]
            (let [window (find-window (.getDocument fb))
                  event {:doc (.getDocument fb) :ch "«" :offset offset :window window}]
              (if (cmd?)
                (handle-chords event)
                (do (proxy-super remove fb offset length)
                  (refresh-tag-text (set-dirty window true))))))

    (replace [fb offset length text attr]
             (let [window (find-window (.getDocument fb))
                   event {:doc (.getDocument fb) :ch text :offset offset :window window}]
               (if (cmd?)
                 (handle-chords event)
                 (do (proxy-super replace fb offset length text attr)
                   (refresh-tag-text (set-dirty window true))))))))

;; UI elements

(defn add-keybindings [window]
  (let [imap (.getInputMap (:body window) JComponent/WHEN_IN_FOCUSED_WINDOW)]
    (.put imap (KeyStroke/getKeyStroke KeyEvent/VK_ESCAPE 0) esc-action)))

(defn make-text-pane []
  (let [panel (JTextPane.)]
    (.setFont panel (Font. Font/MONOSPACED Font/PLAIN 14))
    panel))

(defn make-window []
  (let [panel (JPanel.)
        tag (make-text-pane)
        body (make-text-pane)
        wid (get-next-wid)]
    (.setPreferredSize tag (Dimension. 500 20))
    (.setLayout panel (BorderLayout.))
    (.add panel (JScrollPane. tag) BorderLayout/PAGE_START)
    (.add panel (JScrollPane. body) BorderLayout/CENTER)
    {:panel panel :tag tag :body body :wid wid :dirty false}))

(defn make-frame []
  (let [frame (JFrame. "Akshar")]
    (doto frame
      (.setSize 200 200)
      (.setDefaultCloseOperation JFrame/EXIT_ON_CLOSE)
      )))

(defn make-editor [fpath]
  (let [frame (make-frame)
        w (make-window)
        window (open fpath w)]
    (.add (.getContentPane frame) (:panel window) BorderLayout/CENTER)
    (.setDocumentFilter (.getDocument (:body window)) doc-filter)
    (.putProperty (.getDocument (:body window)) "wid" (:wid window))
    (add-keybindings window)
    (.pack frame)
    {:frame frame
     :windows {(:wid window) window}}))

(defn start [fpath]
  (let [editor (make-editor fpath)]
    (.setVisible (:frame editor) true)
    (swap! state merge editor)
    (insert-mode)))

(defn -main
  [& args]
  (if-let [fpath (first args)]
    (start fpath)
    (start fs/*cwd*)))
