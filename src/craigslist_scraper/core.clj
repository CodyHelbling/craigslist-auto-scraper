(ns craigslist-scraper.core
  (:require
   [hickory.select :as s]
   [clj-http.client :as client]
   [clojure.string :as string]
   [clojure.pprint :as pp])
  (:use
   hickory.core)
  (:gen-class))

(def url-craigslist-sites "https://craigslist.org/about/sites#US")

(defn get-craigslist-us-sites []
  (println "get-craigslist-us-sites")
  (let [htree (-> (client/get url-craigslist-sites) :body parse as-hickory)]
        (pp/pprint (:content (nth (:content (nth (:content (nth (:content (nth (:content (nth (:content htree) 1)) 2)) 3)) 3)) 7)))))

(defn trim-attr-group
  "Scrape the attributes from the side column of craigslist. e.g. vin, transmission, cylinders..."
  ([attr-group]
     (if (> (count attr-group) 0)
       (let [attribute-name    (first (:content (nth attr-group 1)))
             attribute-content (first (:content (nth (:content (nth attr-group 1))1)))]
         (if attribute-name
           (trim-attr-group attr-group {(string/lower-case (keyword attribute-name)) attribute-content} 1)
           (trim-attr-group attr-group {} 1)))
         {}))
  ([attr-group attributes index]
   (if (> (count attr-group) index)
     (do
       (let [attribute-name    (first (:content (nth attr-group index)))
             attribute-content (first (:content (nth   (:content (nth attr-group index))1)))]
         ; (println index "- attribute-name: " attribute-name)
         ; (pp/pprint  attributes)
         (if attribute-name
           (trim-attr-group attr-group (conj attributes {(string/lower-case (keyword attribute-name)) attribute-content}) (inc index))
           (trim-attr-group attr-group attributes (inc index)))))
     attributes)))


(def url "https://boise.craigslist.org/cto/d/2006-audi-a6-32-quattro/6359027948.html")
(def site-htree   (-> (client/get url) :body parse as-hickory))
(def title        (first ( :content (first (-> (s/select (s/descendant (s/id "titletextonly")) site-htree))))))
(def description  (last ( :content (first (-> (s/select (s/descendant (s/id "postingbody")) site-htree))))))
(def summary      (first (:content (first (:content (nth (:content (first (-> (s/select (s/descendant (s/class "attrgroup")) site-htree))))1))))))
(def postinginfos ( :content (first (-> (s/select (s/descendant (s/class "postinginfos")) site-htree)))))
(def postdate     (first (:content (first (-> (s/select (s/descendant (s/class "timeago")) site-htree))))))
(def price        (first (:content (first (-> (s/select (s/descendant (s/class "price")) site-htree))))))
(def attrgroup    (:content (nth (-> (s/select (s/descendant (s/class "attrgroup")) site-htree))1)))


(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (let [craigslist-sites (get-craigslist-us-sites)])
  (let [auto {:title title
              :description description
              :summary summary
              :postid (subs (first (last (last (nth postinginfos 1))))9)
              :postdate postdate
              :price price
              :auto-attributes (trim-attr-group attrgroup)}]
    (pp/pprint auto)))
  ; (trim-attr-group attrgroup)))
