(ns psdm-client.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [goog.string :as gstring]
            [clojure.string :as str]
            [cljs.reader :as reader]
            [goog.net.XhrIo :as xhrio]
            [goog.string.format]))

(enable-console-print!)

(def app-state (atom {:current-list nil
                      :lists []}))

;; test data
#_(reset! app-state {:current-list nil
                   :lists        [{:name  "Todo List of things todo"
                                   :id    0
                                   :self  "/api/0"
                                   :items [{:self "" :status :done :description "Take 1"}
                                           {:self "" :status :done :description "Task 2"}]}
                                  {:name  "Another list"
                                   :id    1
                                   :self  "/api/1"
                                   :items [{:self "" :status :done :description "Take 1"}
                                           {:self "" :status :todo :description "Task 2"}]}]})

(defn read-edn-from-event [e]
  (-> (.-target e)
      (.getResponseText)
      reader/read-string))

(defn load-todo-list [todo-list-url todo-list-cursor]
  (xhrio/send todo-list-url
              (fn [e]
                (let [data (read-edn-from-event e)]
                  (om/transact! todo-list-cursor
                                (fn [oldstate]
                                  (merge oldstate data)))))))

(defn load-todo-lists [todo-lists-url todo-lists-cursor]
  (xhrio/send todo-lists-url
              (fn [e]
                (let [data (read-edn-from-event e)]
                  (om/transact! todo-lists-cursor
                                (fn [oldstate]
                                  (assoc oldstate
                                    :lists (vec (map #(assoc % :items []) data)))))))))

(defn update-todo-item [todo-item todo-item-cursor]
  (xhrio/send (:self todo-item)
              (fn [e]
                (let [data (read-edn-from-event e)]
                  (om/transact! todo-item-cursor
                                (fn [_]
                                  data))))
              "PUT"
              (pr-str todo-item)
              #js {:Content-Type "application/edn"}))

(defn delete-todo-item [todo-item todo-list-cursor]
  (xhrio/send (:self todo-item)
              (fn [e]
                (om/transact! todo-list-cursor
                              (fn [olddata]
                                (update-in olddata [:items]
                                           (fn [items]
                                             (vec (remove #(= (:id todo-item) (:id %)) items)))))))
              "DELETE"))

(defn delete-todo-list [todo-list app-cursor]
  (xhrio/send (:self todo-list)
              (fn [e]
                (om/transact! app-cursor
                              (fn [olddata]
                                (update-in olddata [:lists]
                                           (fn [lists]
                                             (vec (remove #(= (:id todo-list) (:id %)) lists)))))))
              "DELETE"))

(defn create-todo-item [todo-item todo-list-cursor]
  (xhrio/send (:self todo-list-cursor)
              (fn [e]
                (let [data (read-edn-from-event e)]
                  (om/transact! todo-list-cursor
                                (fn [olddata]
                                  (update-in olddata [:items] conj data)))))
              "POST"
              (pr-str todo-item)
              #js {:Content-Type "application/edn"}))

(defn create-todo-list [todo-list app-cursor]
  (xhrio/send "/api/"
              (fn [e]
                (let [data (read-edn-from-event e)]
                  (om/transact! app-cursor
                                (fn [olddata]
                                  (update-in olddata [:lists] conj data)))))
              "POST"
              (pr-str todo-list)
              #js {:Content-Type "application/edn"}))

(defn item-status-toggle-id [item]
  (str "todo_list_item_checkbox_" (:id item)))

(def opposite-status {:done :todo
                      :todo :done})

;; Passing the parent's cursor down is bad, but somehow we have to get access
;; to it for deleting an item. The better way to do this is with core.async, but
;; I'm trying to avoid that for the time being (might add it later if there is
;; time).
(defn todo-item-view [todo-list-cursor todo-item owner]
  (reify
    om/IRender
    (render [this]
      (dom/li #js {:className "list-group-item"}
              (dom/span #js {:className "pull-right action-buttons"}
                        (dom/a #js {:className "trash"
                                    :href      "#"
                                    :onClick   (fn [_]
                                                 (delete-todo-item todo-item todo-list-cursor))}
                               (dom/span #js {:className "glyphicon glyphicon-trash"})))
              (dom/div #js {:className "checkbox"}
                       (dom/input #js {:checked   (= :done (:status todo-item))
                                       :className "strikethrough"
                                       :type      "checkbox"
                                       :id        (item-status-toggle-id todo-item)
                                       :onClick   (fn [_]
                                                    (update-todo-item (assoc todo-item
                                                                        :status (opposite-status (:status todo-item)))
                                                                      todo-item))})
                       (dom/label #js {:htmlFor (item-status-toggle-id todo-item)}
                                  (:description todo-item)))))))

(defn todo-items-view [todo-items-page owner]
  (reify
    om/IRender
    (render [this]
      (let [on-submit (fn [e]
                        (let [description (-> e
                                              (.-target)
                                              (aget "description")
                                              (aget "value"))]
                          (create-todo-item {:description description} todo-items-page)
                          (.preventDefault e)))]
        (dom/div nil
                 (dom/h2 nil (:name todo-items-page))
                 (dom/form #js {:action   (:self todo-items-page)
                                :method   "post"
                                :role     "form"
                                :onSubmit on-submit}
                           (dom/div #js {:className "input-group"}
                                    (dom/input #js {:className   "form-control"
                                                    :type        "text"
                                                    :name        "description"
                                                    :maxLength   100
                                                    :placeholder "Create a new thing TODO..."
                                                    :autofocus   true})
                                    (dom/span #js {:className "input-group-btn"}
                                              (dom/input #js {:className "btn btn-default"
                                                              :type      "submit"
                                                              :value     "Add Item"}))))
                 (apply dom/ul
                        #js {:className "list-group"}
                        (om/build-all (partial todo-item-view todo-items-page)
                                      (:items todo-items-page))))))))

(defn todo-list-view [app todo-list owner]
  (reify
    om/IRender
    (render [this]
      (let [to-list (fn [_]
                      ;; first update the list pointer to the selected list
                      (om/transact! app
                                    (fn [oldvalue]
                                      (assoc oldvalue
                                        :current-list (:self todo-list))))
                      ;; reload the list (to get the items)
                      (load-todo-list (:self todo-list) todo-list))
            delete (fn [_]
                     (delete-todo-list todo-list app))]
        (dom/li #js {:className "list-group-item"}
                (dom/span #js {:className "pull-right action-buttons"}
                          (dom/a #js {:href "#" :className "trash"
                                      :onClick delete}
                                 (dom/span #js {:className "glyphicon glyphicon-trash"})))
                (dom/span nil
                          (dom/a #js {:href "#" :onClick to-list}
                                 (:name todo-list))))))))

(defn todo-lists-view [app owner]
  (reify
    om/IRender
    (render [this]
      (let [on-submit (fn [e]
                        (let [list-name (-> e
                                            (.-target)
                                            (aget "name")
                                            (aget "value"))]
                          (create-todo-list {:name list-name} app)
                          (.preventDefault e)))]
        (dom/div nil
                 (dom/h1 nil "TODO Lists")
                 (dom/form #js {:action   "/api/"
                                :method   "post"
                                :role     "form"
                                :onSubmit on-submit}
                           (dom/div #js {:className "input-group"}
                                    (dom/input #js {:className   "form-control"
                                                    :type        "text"
                                                    :name        "name"
                                                    :maxLength   100
                                                    :placeholder "Create a new TODO list..."
                                                    :autofocus   true})
                                    (dom/span #js {:className "input-group-btn"}
                                              (dom/input #js {:className "btn btn-default"
                                                              :type      "submit"
                                                              :value     "Add List"}))))
                 (apply dom/ul
                        #js {:className "list-group"}
                        (om/build-all (partial todo-list-view app)
                                      (:lists app))))))))

(defn app-view [app owner]
  (reify
    om/IRender
    (render [this]
      (if-let [current-list-uri (:current-list app)]
        (let [current-list (first (filter #(= current-list-uri (:self %))
                                          (:lists app)))]
          (om/build todo-items-view current-list))
        (om/build todo-lists-view app)))))

(defn insert-root-component! [target]
  (om/root app-view
           app-state
           {:target target}))

(insert-root-component! (.getElementById js/document "om-root"))

(load-todo-lists "/api/" (om/root-cursor app-state))
