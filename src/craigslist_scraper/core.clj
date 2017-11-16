(ns craigslist-scraper.core
  (:require
   [hickory.select :as s]
   [clj-http.client :as client]
   [clojure.string :as string]
   [clojure.pprint :as pp])
  (:use
   hickory.core)
  (:gen-class))

;; Scrape Auto Details
(def url "https://boise.craigslist.org/cto/d/2006-audi-a6-32-quattro/6359027948.html")
(def site-htree   (-> (client/get url) :body parse as-hickory))
(def title        (first ( :content (first (-> (s/select (s/descendant (s/id "titletextonly")) site-htree))))))
(def description  (last ( :content (first (-> (s/select (s/descendant (s/id "postingbody")) site-htree))))))
(def summary      (first (:content (first (:content (nth (:content (first (-> (s/select (s/descendant (s/class "attrgroup")) site-htree))))1))))))
(def postinginfos ( :content (first (-> (s/select (s/descendant (s/class "postinginfos")) site-htree)))))
(def postdate     (first (:content (first (-> (s/select (s/descendant (s/class "timeago")) site-htree))))))
(def price        (first (:content (first (-> (s/select (s/descendant (s/class "price")) site-htree))))))
(def attrgroup    (:content (nth (-> (s/select (s/descendant (s/class "attrgroup")) site-htree))1)))

;; Scrape Auto Links From Site
(def url-craigslist-autos "https://boise.craigslist.org/search/cta")
(def craigslist-autos-htree (-> (client/get url-craigslist-autos) :body parse as-hickory))
(def craigslist-autos-links (-> (s/select (s/descendant (s/class "hdrlnk")) craigslist-autos-htree)))

;; Scrape Craiglist's United States Links
(def url-craigslist-sites "https://craigslist.org/about/sites#US")
(def craigslist-sites-htree (-> (client/get url-craigslist-sites) :body parse as-hickory))
(def craigslist-links (-> (s/select (s/descendant (s/attr "href")) (nth (:content (nth (:content (nth (:content (nth (:content (nth (:content craigslist-sites-htree) 1)) 2)) 3)) 3)) 7))))

(defn get-craigslist-autos
  ; Move into craigslist scraper namespace
  ([hickory-links]
   (println "get-craigslist-autos")
   (if (> (count hickory-links) 0)
     (let [auto-name (first (:content (first hickory-links)))
           auto-link (:href (:attrs (first hickory-links)))
           auto-id   (:data-id (:attrs (first hickory-links)))]
       (if auto-link
         (get-craigslist-autos hickory-links [{:auto-name auto-name :auto-link auto-link :auto-id auto-id}] 1)
         (get-craigslist-autos hickory-links [] 1)))
     []))
  ([hickory-links autos index]
   (if (> (count hickory-links) index)
     (let [auto-name (first (:content  (nth hickory-links index)))
           auto-link (:href (:attrs    (nth hickory-links index)))
           auto-id   (:data-id (:attrs (nth hickory-links index)))]
       ; (println "auto-name: " auto-name)
       ; (println "auto-link: " auto-link)
       ; (println "index:     " index)
       ; (println "count:     " (count hickory-links))
       ; (println "autos:     " autos)
       (if auto-link
         (get-craigslist-autos hickory-links (conj autos {:auto-name auto-name :auto-link auto-link :auto-id auto-id}) (inc index))
         (get-craigslist-autos hickory-links autos  (inc index))))
     autos)))

(defn get-craigslist-us-sites
  ; Move this into craigslist scraper namespace
  ([hickory-links]
   (println "get-craigslist-us-sites")
   (println (first (:content (first hickory-links))))
   (println (:href (:attrs (first hickory-links))))
   (if (> (count hickory-links) 0)
          (let [site-name (first (:content (first hickory-links)))
                site-link (:href (:attrs   (first hickory-links)))]
            (if site-link
              (get-craigslist-us-sites hickory-links [{:site-name site-name :site-link site-link}] 1)
              (get-craigslist-us-sites hickory-links [] 1)))
          []))
  ([hickory-links sites index]
   (if (> (count hickory-links) index)
     (let [site-name (first (:content (nth hickory-links index)))
           site-link (:href (:attrs   (nth hickory-links index)))]
       ; (println "site-name: " site-name)
       ; (println "site-link: " site-link)
       ; (println "index:     " index)
       ; (println "count:     " (count hickory-links))
       ; (println "sites:     " sites)
       (if site-link
         (get-craigslist-us-sites hickory-links (conj sites {:site-name site-name :site-link site-link}) (inc index))
         (get-craigslist-us-sites hickory-links sites  (inc index))))
     sites)))
     
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

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (let [craigslist-sites (get-craigslist-us-sites craigslist-links)]
      (pp/pprint craigslist-sites))
  (let [auto {:title title
              :description description
              :summary summary
              :postid (subs (first (last (last (nth postinginfos 1))))9)
              :postdate postdate
              :price price
              :auto-attributes (trim-attr-group attrgroup)}]
    (pp/pprint auto))
  (pp/pprint (get-craigslist-autos craigslist-autos-links)))
