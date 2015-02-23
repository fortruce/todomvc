(ns todomvc.core
    (:require [reagent.core :as reagent :refer [atom]]
              [cljsjs.react :as react]))

;; -------------------------
;; Views

;; Use sorted map so all todos are in order by addition
(defonce app-state
  (atom (sorted-map)))

;; Todo counter to create ids
(def counter (atom 0))

;;
;; App-State modification functions
;;
(defn add-todo!
  "Add a todo to the app-state with text"
  [text]
  (let [id (swap! counter inc)]
    (swap! app-state assoc id {:id id :text text :done false})))

(defn toggle!
  "Toggle the :done status of todo by id"
  [id]
  (swap! app-state update-in [id :done] not))

(defn remove-todo!
  "Remove todo by id"
  [id]
  (swap! app-state dissoc id))

(defn save!
  "Save edits to todo's title by id"
  [id text]
  (swap! app-state assoc-in [id :text] text))

;;
;; Create initial app state
;;
(defonce init (do
                (add-todo! "Create Todo App")
                (add-todo! "Run 4 miles")))

(defn todo-input
  "Input box for todos. Pass in initial title and a callback for on-save."
  [{:keys [text on-save]}]
  (let [val (atom text)
        save #(let [v (-> @val str clojure.string/trim)]
               (if-not (empty? v)
                 (on-save v)))]
    (fn []
      [:input {:type "text"
               :value @val
               :on-blur save
               :on-change #(reset! val (.. % -target -value))
               :on-key-down #(case (.-which %)
                               13 (save)
                               nil)}])))

(def todo-edit
  "A todo-input with focus when spawned."
  (with-meta todo-input {:component-did-mount #(.focus (reagent/dom-node %))}))

(defn todo-entry []
  (let [editing (atom false)]
    (fn [{:keys [id text done]}]
      [:li {:class (str (if done "completed ")
                        (if @editing "editing"))}
       [:div
        [:input {:type "checkbox"
                 :on-change #(toggle! id)}]
        [:label {:on-double-click #(reset! editing true)} text]
        [:button {:on-click #(remove-todo! id)} "X"]]
       (when @editing
         [todo-edit {:text text
                      :on-save #(do (save! id %)
                                    (reset! editing false))}])])))

(defn todo-app [_]
  (fn []
    [:ul
     (for [todo (vals @app-state)]
       ^{:key (:id todo)} [todo-entry todo])]))

  ;; -------------------------
  ;; Initialize app

(defn init! []
  (reagent/render-component [todo-app] (.getElementById js/document "app")))
