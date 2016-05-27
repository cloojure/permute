(defproject permute "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :main permute.core
  :dependencies [ [org.clojure/clojure              "1.8.0"]
                  [org.clojure/core.incubator       "0.1.3"]
                  [org.clojure/core.async           "0.2.374"]
                  [org.clojure/test.check           "0.9.0"]
                  [org.clojure/core.match           "0.3.0-alpha4"]
                  [org.clojure/math.combinatorics   "0.1.2"]
                  [clojure-csv/clojure-csv          "2.0.2"]
                  [clj-time                         "0.11.0"]
                  [criterium                        "0.4.4"]
                  [cheshire                         "5.6.1"]
                  [prismatic/schema                 "1.1.1"]
                ]
  :plugins  [ [lein-codox "0.9.4"] ]
  :codox {:src-dir-uri "http://github.com/cloojure/tupelo/blob/master/"
          :src-linenum-anchor-prefix "L"}
  :deploy-repositories {  "snapshots" :clojars
                          "releases"  :clojars }
  :update :always; :daily  
  :target-path "target/%s"
  :clean-targets [ "target" ]
  :profiles { ; :dev      { :certificates ["clojars.pom"] }
              :uberjar  { :aot :all }
            }
  :global-vars { *warn-on-reflection* false }

  ; "lein test"         will not  run tests marked with the ":slow" metadata
  ; "lein test :slow"   will only run tests marked with the ":slow" metadata
  ; "lein test :all"    will run all  tests (built-in)
  :test-selectors { :default    (complement :slow)
                    :slow       :slow }

  :jvm-opts ^:replace ["-Xms1g" "-Xmx4g" ]
)
