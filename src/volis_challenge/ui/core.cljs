(ns volis-challenge.ui.core)

(defn mount-root []
  (when-let [el (.getElementById js/document "app")]
    (set! (.-innerHTML el) "<h1>Volis Challenge</h1><p>Frontend ClojureScript inicial.</p>")))

(defn ^:export init []
  (mount-root))

