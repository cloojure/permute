(ns tst.permute.core
  (:use permute.core
        tupelo.core
        clojure.test )
  (:require [clojure.string   :as str]
            [schema.core      :as s]
            [tupelo.misc      :as misc]
            [clojure.math.combinatorics  :as combo]
            [criterium.core :as crit]
  ))

; Prismatic Schema type definitions
(s/set-fn-validation! true)   ; #todo add to Schema docs

(set! *warn-on-reflection* true)

(deftest t-permute-multiset

; (is (=  (permute-multiset-1 [        ])  [])  ; #todo should be error
  (is (=  (permute-multiset-1 [:a      ])  [[:a]] ))
  (is (=  (permute-multiset-1 [:a :b   ])  [[:a :b] [:b :a]] ))
  (is (=  (permute-multiset-1 [:a :b :c])  [[:a :b :c] [:a :c :b]
                                            [:b :a :c] [:b :c :a]
                                            [:c :a :b] [:c :b :a]] ))

; (is (=  (permute-multiset-2 [        ])  []))  ; #todo should be error
  (is (=  (permute-multiset-2 [:a      ])  [[:a]] ))
  (is (=  (permute-multiset-2 [:a :b   ])  [[:a :b] [:b :a]] ))
  (is (=  (permute-multiset-2 [:a :b :c])  [[:a :b :c] [:a :c :b]
                                            [:b :a :c] [:b :c :a]
                                            [:c :a :b] [:c :b :a]] ))

; (is (=  (permute-multiset-3 [        ])  [[]]))  ; #todo should be error
  (is (=  (permute-multiset-3 [:a      ])  [[:a]] ))
  (is (=  (permute-multiset-3 [:a :b   ])  [[:a :b] [:b :a]] ))
  (is (=  (permute-multiset-3 [:a :b :c])  [[:a :b :c] [:a :c :b]
                                            [:b :a :c] [:b :c :a]
                                            [:c :a :b] [:c :b :a]] ))
)

(deftest t-permute-set
  (is (= (permute-set-1 #{:a      }) #{ [:a      ]            } ))
  (is (= (permute-set-1 #{:a :b   }) #{ [:a :b   ] [:b :a   ] } ))
  (is (= (permute-set-1 #{:a :b :c}) #{ [:a :b :c] [:a :c :b]
                                        [:b :a :c] [:b :c :a]
                                        [:c :a :b] [:c :b :a] } ))
; (is (thrown? IllegalArgumentException (permute-set-1 #{} )))
)

(deftest t-permute-lazy
  (is (=  (permute-lazy-1 [:a      ])  [[:a]] ))
  (is (=  (permute-lazy-1 [:a :b   ])  [[:a :b] [:b :a]] ))
  (is (=  (permute-lazy-1 [:a :b :c])  [[:a :b :c] [:a :c :b]
                                        [:b :a :c] [:b :c :a]
                                        [:c :a :b] [:c :b :a]] ))
  (is (thrown? IllegalArgumentException (permute [] ))))

(deftest t-permute-tail-1
  (is (= (set (permute-tail-1 [:a]))        #{ [:a      ]            } ))
  (is (= (set (permute-tail-1 [:a :b]))     #{ [:a :b   ] [:b :a   ] } ))
  (is (= (set (permute-tail-1 [:a :b :c]))  #{ [:a :b :c] [:a :c :b]
                                               [:b :a :c] [:b :c :a]
                                               [:c :a :b] [:c :b :a] } )))
(deftest t-permute-tail-2
  (is (= (set (permute-tail-2 [:a]))        #{ [:a      ]            } ))
  (is (= (set (permute-tail-2 [:a :b]))     #{ [:a :b   ] [:b :a   ] } ))
  (is (= (set (permute-tail-2 [:a :b :c]))  #{ [:a :b :c] [:a :c :b]
                                               [:b :a :c] [:b :c :a]
                                               [:c :a :b] [:c :b :a] } )))
(deftest t-permute-tail-3
  (is (= (set (permute-tail-3 [:a]))        #{ [:a      ]            } ))
  (is (= (set (permute-tail-3 [:a :b]))     #{ [:a :b   ] [:b :a   ] } ))
  (is (= (set (permute-tail-3 [:a :b :c]))  #{ [:a :b :c] [:a :c :b]
                                               [:b :a :c] [:b :c :a]
                                               [:c :a :b] [:c :b :a] } )))

(deftest t-permute-gen
  (is (thrown? IllegalArgumentException (permute [] )))

  (is (= (set (permute [:a]))        #{ [:a      ]            } ))
; (newline) (newline)
  (is (= (set (permute [:a :b]))     #{ [:a :b   ] [:b :a   ] } ))
; (newline) (newline)
  (is (= (set (permute [:a :b :c]))  #{ [:a :b :c] [:a :c :b]
                                        [:b :a :c] [:b :c :a]
                                        [:c :a :b] [:c :b :a] } ))
; (newline) (newline)
; (pretty (permute [:a :b :c :d]))
  (is (= (set (permute [:a :b :c :d]))
         #{ [ :a :b :c :d ]
            [ :a :b :d :c ]
            [ :a :c :b :d ]
            [ :a :c :d :b ]
            [ :a :d :b :c ]
            [ :a :d :c :b ]

            [ :b :a :c :d ]
            [ :b :a :d :c ]
            [ :b :c :a :d ]
            [ :b :c :d :a ]
            [ :b :d :a :c ]
            [ :b :d :c :a ]

            [ :c :a :b :d ]
            [ :c :a :d :b ]
            [ :c :b :a :d ]
            [ :c :b :d :a ]
            [ :c :d :a :b ]
            [ :c :d :b :a ]

            [ :d :a :b :c ]
            [ :d :a :c :b ]
            [ :d :b :a :c ]
            [ :d :b :c :a ]
            [ :d :c :a :b ]
            [ :d :c :b :a ] } ))
)

(deftest t-permute-lazy-count
  (let [
        check-same-perm     (fn [n]
                              (println "checking n=" n)
                              (is (= (into #{} (combo/permutations (thru 1 n)))
                                     (into #{} (permute-multiset-1 (thru 1 n))))))
    ]

    (check-same-perm  1)
    (check-same-perm  2)
    (check-same-perm  3)
    (check-same-perm  4)
    (check-same-perm  5)
    (check-same-perm  6)
    (check-same-perm  7)
    (check-same-perm  8)
  ))

(deftest t-permute-nest-timing
  (let [size 6]
    (newline)
    (println "size=" size)

    (newline)
    (println "permute-multiset-1")
    (crit/quick-bench (into [] (permute-multiset-1 (thru 1 size))))

    (newline)
    (println "permute-multiset-2")
    (crit/quick-bench (into [] (permute-multiset-2 (thru 1 size))))

    (newline)
    (println "permute-multiset-3")
    (crit/quick-bench (into [] (permute-multiset-3 (thru 1 size))))

    (newline)
    (println "permute-set-1")
    (crit/quick-bench (into [] (permute-set-1 (set (thru 1 size)))))

    (newline)
    (println "permute-tail-1")
    (crit/quick-bench (into [] (permute-tail-1 (thru 1 size))))

    (newline)
    (println "permute-tail-2")
    (crit/quick-bench (into [] (permute-tail-2 (thru 1 size))))

    (newline)
    (println "permute-tail-3")
    (crit/quick-bench (into [] (permute-tail-3 (thru 1 size))))

    (newline)
    (println "permute-lazy-1")
    (crit/quick-bench (into [] (permute-lazy-1 (thru 1 size))))

    (newline)
    (println "permute-nest-1")
    (crit/quick-bench (into [] (permute-nest-1 (thru 1 size))))

    (newline)
    (println "permute-nest-2")
    (crit/quick-bench (into [] (permute-nest-2 (thru 1 size))))

    (newline)
    (println "combo/permutations")
    (crit/quick-bench (into [] (combo/permutations (thru 1 size))))
))


(deftest t-permute-nest-1
  (is (set (permute-nest-1 [:a]))        #{ [:a      ]            } )
  (is (set (permute-nest-1 [:a :b]))     #{ [:a :b   ] [:b :a   ] } )
  (is (set (permute-nest-1 [:a :b :c]))  #{ [:a :b :c] [:a :c :b]
                                            [:b :a :c] [:b :c :a]
                                            [:c :a :b] [:c :b :a] } )
)
