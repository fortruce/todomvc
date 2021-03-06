(ns todomvc.core
  (:require [reagent.core :as reagent :refer [atom]]
            [cljsjs.react :as react]))

;; -------------------------
;; Views

;; Use sorted map so all todos are in order by addition
(defonce app-state
  (atom (sorted-map)))

;; Todo counter to create ids
(defonce counter (atom 0))

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

(defn focus [component]
  (.focus (reagent/dom-node component)))

(defn atom-input
  ([value] (atom-input value nil))
  ([value props]
   [:input (merge
            {:type "text"
             :value @value
             :on-focus #(set! (-> % .-target .-value) @value)
             :on-change #(reset! value (-> % .-target .-value))}
            props)]))

(defn todo-input
  "Input box for todos. Pass in initial title and a callback for on-save."
  [{:keys [text on-save]}]
  (reagent/create-class {:render
                         (let [val (atom text)
                               save #(let [v (clojure.string/trim @val)]
                                       (if-not (empty? v)
                                         (on-save v)))]
                           (fn []
                             (atom-input val
                                         {:on-blur save
                                          :on-key-down #(case (.-which %)
                                                          13 (save)
                                                          nil)})))
                         :component-did-mount focus}))

(defn todo-entry []
  (let [editing (atom false)]
    (fn [{:keys [id text done]}]
      [:li {:class (if @editing "editing")}
       [:div.todo-entry
        [:input {:type "checkbox"
                 :on-change #(toggle! id)}]
        [:label {:on-double-click #(reset! editing true)} text]
        [:a.destroy {:on-click #(remove-todo! id)}]]
       (when @editing
         [todo-input {:text text
                      :on-save #(do (save! id %)
                                    (reset! editing false))}])])))


(defn new-todo []
  (let [todo (atom "")]
    (reagent/create-class {:render
                           (fn []
                             (atom-input
                              todo
                              {:id "new-todo"
                               :placeholder "What needs to be done?"
                               :on-key-down #(case (.-which %)
                                               13 ((fn [_]
                                                     (if-not (empty? (clojure.string/trim @todo))
                                                       (do
                                                         (add-todo! @todo)
                                                         (reset! todo "")))))
                                               nil)}))
                           :component-did-mount focus})))

(defn todo-app [_]
  (fn []
    [:div#todo-app
     [:header
      [:h1#header "todos"]
      [new-todo]]
     [:ul#todo-list
      (for [todo (vals @app-state)]
        ^{:key (:id todo)} [todo-entry todo])]]))

  ;; -------------------------
  ;; Initialize app

(defn init! []
  (reagent/render-component [todo-app] (.getElementById js/document "app")))
