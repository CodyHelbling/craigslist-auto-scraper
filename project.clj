(defproject craigslist-scraper "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [enlive "1.1.6"]
                 [com.github.kyleburton/clj-xpath "1.4.11"]
                 [hickory "0.7.1"]
                 [clj-http "3.7.0"]]
  :main ^:skip-aot craigslist-scraper.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
