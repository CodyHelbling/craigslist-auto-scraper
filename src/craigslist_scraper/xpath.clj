;; This is an exmaple of using clj-xpath.  We'll grab an RSS
;; feed (an XML document) and we'lll use clj-xpath to explore the
;; structure of the document as well as extract its contents.

(ns clj-xpath-examples.core
  (:require
   [clojure.string :as string]
   [clojure.pprint :as pp])
  (:use
   clj-xpath.core))

;; Lets define an URL to an RSS feed...
(def hackernews-rss-url "http://www.codyhelbling.com")

;; Now lets fetch the RSS, remembering it so we don't fetch it more
;; than once (hitting Hacker News multiple times would be rude).
(def hackernews-rss-xml
     (memoize (fn [] (slurp hackernews-rss-url))))

(comment
  ;; We can see that the XML loaded by asking for the character count:
  (count (hackernews-rss-xml))
  ;;  => 9968
  )

;; Now lets parse the XML into a Document.  It is not strictly
;; necessary to memoize, but this provides a cache of the parsed
;; document which will make operations on the document faster.
(def xmldoc
     (memoize (fn [] (xml->doc (hackernews-rss-xml)))))

(comment
  ;; With a document, we can now perform some XPath operations on the document:

  ;; extract the root node:
  ;; NB: I'm removing :text (the dissoc) because it's large and not very useful at this point...
  (->
   ($x "/*" (xmldoc))
   first
   (dissoc :text))
  ;; => {:node #<DeferredElementImpl [rss: null]>, :tag :rss, :attrs {:version "2.0"}}

  ;; extract the root node's tag:
  ($x:tag "/*" (xmldoc))
  ;; => :rss

  ;; extract the channel's title link and description:
  ($x:text "/rss/channel/title" (xmldoc))
  ;; => "Hacker News"

  ($x:text "/rss/channel/link" (xmldoc))
  ;; => "http://news.ycombinator.com/"

  ($x:text "/rss/channel/description" (xmldoc))
  ;; => "Links for the intellectually curious, ranked by readers."

  ;; If you're not intimately familiar with the structure of your XML
  ;; seeing what it looks like can be a pain, so we'll define a few
  ;; more helper functions...
  )

;; Traverses the entire document, returning a distincted list of the
;; tags (in no particular order).
(defn all-tags [doc]
  (map
   ;; turn the keywords into strings
   name
   (seq
    ;; reduce the stream of nodes into a distinct list
    (reduce
     (fn [acc node]
       (conj acc (:tag node)))
     #{}
     ;; tree-seq flattens the document into a one-dimensional stream
     ;; of nodes:
     (tree-seq (fn [n] (:node n))
               (fn [n] ($x "./*" n))
               (first ($x "./*" doc)))))))

(comment

 ;; Lets use that to see what all the tags are:
 (all-tags (xmldoc))
 ;; ("link" "item" "title" "channel" "rss" "comments" "description")

 ;; That helps a bit, but it's still not too useful, as it doesn't
 ;; show us how they nest.  Lets try another helper function...
 )

;; Better would be to visit each node in the document, keeping track
;; of the path.  The callback to this function will be passed 2
;; arguments:
;;  * path: the path to the node being visited
;;  * node: the node being visited
(defn visit-nodes
  ([path nodes f]
     (vec
      (mapcat
       #(vec
         (cons
          ;; invoke the callback on the each of the nodes
          (f (conj path (:tag %1)) %1)
          ;; visit each of the children of this node
          (visit-nodes
           (conj path (:tag %1))
           ($x "./*" %1) f)))
       nodes))))

(comment
  ;; Visit each part of the document (tree) and print out what the path to each tag looks like:
  (visit-nodes []
               ($x "./*" (xmldoc))
               (fn [p n]
                 (printf "%s tag:%s\n"
                         (apply str (interpose "/" (map name p)))
                         (name (:tag n)))))
  ;; Here's the output from the above:
  ;;     rss tag:rss
  ;;     rss/channel tag:channel
  ;;     rss/channel/title tag:title
  ;;     rss/channel/link tag:link
  ;;     rss/channel/description tag:description
  ;;     rss/channel/item tag:item

  )

;; This is pretty much what we want, lets wrap it up into a
;; funciton...
(defn all-paths [doc]
  (map
   #(str "/" (string/join "/" (map name %1)))
   (first
    (reduce
     (fn [[acc set] p]
       (if (contains? set p)
         [acc set]
         [(conj acc p) (conj set p)]))
     [[] #{}]
     (visit-nodes []
                  ($x "./*" doc)
                  (fn [p n]
                    p))))))

(comment
  ;; Let's try it out:
  (all-paths (xmldoc))
  ;; => ("/rss" "/rss/channel" "/rss/channel/title" "/rss/channel/link" "/rss/channel/description" "/rss/channel/item" "/rss/channel/item/title" "/rss/channel/item/link" "/rss/channel/item/comments" "/rss/channel/item/description")

  ;; Format it bit nicer:
  (doseq [p (all-paths (xmldoc))]
    (println p))

  ;; There, a nice list of all the paths in the document:
  ;;   /rss
  ;;   /rss/channel
  ;;   /rss/channel/title
  ;;   /rss/channel/link
  ;;   /rss/channel/description
  ;;   /rss/channel/item
  ;;   /rss/channel/item/title
  ;;   /rss/channel/item/link
  ;;   /rss/channel/item/comments
  ;;   /rss/channel/item/description


  ;; Now that we can see a bit of the lay of the land, lets grab the first few items' title and link:
  (pp/pprint
   (map
    (fn [item]
      {:title ($x:text "./title" item)
       :link  ($x:text "./link" item)})
    (take 5
          ($x "/rss/channel/item" (xmldoc)))))

  ;; ({:title "Thank HN: Our friend is Safe and Sound"
  ;;   :link
  ;;   "http://jacquesmattheij.com/thank-hn-our-friend-is-safe-and-sound"}
  ;;  {:title "Entrepreneurshit"
  ;;   :link
  ;;   "http://www.bothsidesofthetable.com/2012/11/18/entrepreneurshit-the-blog-post-on-what-its-really-like/?awesm=bothsid.es_i2G&utm_source=t.co&utm_content=awesm-publisher&utm_medium=bothsid.es-twitter&utm_campaign="}
  ;;  {:title "The British Ruby Conference has been cancelled"
  ;;   :link "http://2013.britruby.com"}
  ;;  {:title "Man Arrested At Airport for Unusual Watch"
  ;;   :link
  ;;   "http://depletedcranium.com/man-arrested-at-airport-for-unusual-watch/"}
  ;;  {:title
  ;;   "Textadept: fast, minimalist, and Lua-extensible cross-platform text editor"
  ;;   :link "http://foicica.com/textadept"})



  ;; That concludes the overview of clj-xpath's main features.
  )