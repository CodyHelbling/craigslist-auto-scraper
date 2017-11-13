(ns craigslist-scraper.core
  (:require
   [hickory.select :as s]
   [clj-http.client :as client]
   [clojure.string :as string])
  (:use
   hickory.core)
  (:gen-class))



(defn trim-attr-group
  ([attr-group]
   (let [stuff {}
         attribute (first (:content (nth attr-group 0)))]
     (if (> (count attr-group) 0)
       (if attribute
         (trim-attr-group (conj stuff {(first (:content (nth attr-group 0)))
                                       (first (:content (nth (:content (nth attr-group 0))1)))}
                                )
                          1)
         (trim-attr-group stuff 1))
       {})))
  ([attr-group attrs index]
   (let [attribute (first (:content (nth attr-group index)))]
     (if (> (count attr-group) index)
       (if attribute
         (trim-attr-group (conj attrs {(first (:content (nth attr-group index)))
                                       (first (:content (nth (:content (nth attr-group index))1)))}
                                ) index )
         (trim-attr-group attrs (inc index)))
       attrs))))
     

; (def url "https://boise.craigslist.org/cto/d/2007-audi-a4-20tfsi/6339143320.html")
(def url "https://boise.craigslist.org/cto/d/2006-audi-a6-32-quattro/6359027948.html")
(def site-htree  (-> (client/get url) :body parse as-hickory))
(def title       (first ( :content (first
                                    (-> (s/select
                                         (s/descendant
                                          (s/id "titletextonly"))
                                         site-htree))))))
(def description (last ( :content (first (-> (s/select (s/descendant (s/id "postingbody")) site-htree))))))
(def summary     (first (:content (first (:content (nth (:content (first (-> (s/select (s/descendant (s/class "attrgroup")) site-htree))))1))))))
(def postinginfos      ( :content (first (-> (s/select (s/descendant (s/class "postinginfos")) site-htree)))))
(def postdate (first (:content (first (-> (s/select (s/descendant (s/class "timeago")) site-htree))))))
(def price (first (:content (first (-> (s/select (s/descendant (s/class "price")) site-htree))))))
(def attrgroup   (:content (nth (-> (s/select (s/descendant (s/class "attrgroup")) site-htree))1)))


(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  ; (dorun (map clojure.pprint/pprint site-htree))
  ; (clojure.pprint/pprint (str "title: " title))
  ; (clojure.pprint/pprint (str "description: " description))
  ; (clojure.pprint/pprint (str "summary: " summary))
  ; (clojure.pprint/pprint (str "post id: " (subs (first (last (last (nth postinginfos 1))))9)))
  ; (clojure.pprint/pprint site-htree (clojure.java.io/writer "craigslist.hickory"))
  (let [auto {:title title
              :description description
              :summary summary
              :postid (subs (first (last (last (nth postinginfos 1))))9)
              :postdate postdate
              :price price
              :attrgroup  (get-int attrgroup :content)}]
    (println auto)))
  ; (trim-attr-group attrgroup)))
