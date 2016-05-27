(ns permute.core
  "Miscellaneous functions."
  (:use tupelo.core)
  (:require [clojure.string :as str]
            [clojure.java.shell :as shell]
            [clojure.set :as set]
            [schema.core :as s]
            [tupelo.schema :as ts]
            [tupelo.async :as tas]
            [clojure.core.async :refer [ go go-loop chan buffer close! thread alts! alts!! timeout ]]
  ))

; Prismatic Schema type definitions
(s/set-fn-validation! true)   ; #todo add to Schema docs

(s/defn permute-multiset-1 :- ts/TupleList
  [values :- ts/List]
  (let [num-values (count values)]
    (if (= 1 num-values)
      [values]
      (apply concat
        (for [ii (range num-values)]
          (let [head-val       (nth values ii)
                remaining-vals (drop-at values ii)]
            (for [rest-perm (permute-multiset-1 remaining-vals)]
              (prepend head-val rest-perm))))))))

(s/defn permute-multiset-2 :- ts/TupleList
  [values :- ts/List]
  (let [num-values (count values)]
    (if (= 1 num-values)
      [values]
      (apply concat
        (for [ii (range num-values)]
          (let [head-val       (nth values ii)
                remaining-vals (drop-at values ii)]
            (reduce (fn [accum rest-perm]
                      (append accum
                        (prepend head-val rest-perm)))
              []
              (permute-multiset-2 remaining-vals))))))))

(s/defn permute-multiset-3 :- ts/TupleList
  [values :- ts/List]
  (let [num-values (count values)]
    (if (zero? num-values)
      [[]]
      (apply concat
        (for [ii (range num-values)]
          (let [head-val       (nth values ii)
                remaining-vals (drop-at values ii)]
            (reduce (fn [accum rest-perm]
                      (append accum
                        (glue [head-val] rest-perm)))
              []
              (permute-multiset-3 remaining-vals))))))))

(s/defn permute-set-1 :- #{ts/Vec}
  [values :- #{s/Any} ]
  (if (empty? values)
    #{ [] }
    (apply set/union
      (for [head-val values]
        (let [remaining-vals (disj values head-val)]
          (reduce (fn [accum rest-perm]
                    (conj accum
                      (prepend head-val rest-perm)))
            #{}
            (permute-set-1 remaining-vals)))))))

; fails around N=8
(s/defn permute-lazy-stackoverflow-1
  [values :- ts/List ]
  (let [num-values (count values)]
    (if (= 1 num-values)
      [values]
      (apply concat
        (forv [ii (range num-values)]
          (let [head-val       (nth values ii)
                remaining-vals (drop-at values ii)]
            (reduce (fn [accum rest-perm]
                      (concat (lazy-seq accum)
                        [(lazy-seq (cons head-val rest-perm))]))
              []
              (permute-lazy-stackoverflow-1 remaining-vals))))))))

(s/defn permute-tail-1 :- ts/TupleList
  [values :- ts/List]
  (let [num-values (count values)]
    (if (= 1 num-values)
      [values]
      (let [head-val       (first values)
            remaining-vals (rest values) ]
        (apply concat
          (for [curr-perm (permute-tail-1 remaining-vals)]
            (for [jj (thru 0 (count curr-perm))]
              (concat (take jj curr-perm)
                [head-val]
                (drop jj curr-perm)))))))))

(s/defn permute-tail-2 :- ts/TupleList
  [values :- ts/List]
  (let [num-values (count values)]
    (if (= 1 num-values)
      [values]
      (let [head-val       (first values)
            remaining-vals (rest values) ]
        (apply concat
          (for [curr-perm (permute-tail-2 remaining-vals)]
            (reduce  (fn [accum jj]
                       (conj accum
                         (concat (take jj curr-perm)
                                 [head-val]
                                 (drop jj curr-perm))))
              []
              (thru 0 (count curr-perm)))))))))

(s/defn permute-tail-3 :- ts/TupleList
  [values :- ts/List]
  (if (= 1 (count values))
    [values]
    (let [head-val (first values)]
      (apply concat
        (reduce (fn [accum curr-perm]
                  (conj accum
                    (for [jj (thru 0 (count curr-perm))]
                      (concat (take jj curr-perm)
                              [head-val]
                              (drop jj curr-perm)))))
                []
                (permute-tail-3 (rest values)))))))

(s/defn permute-lazy-1 :- ts/TupleList
  [values :- ts/List ]
  (let [num-values (count values)]
    (if (= 1 num-values)
      [values]
      (apply concat
        (for [ii (range num-values)]
          (let [head-val       (nth values ii)
                remaining-vals (drop-at values ii)]
            (reduce (fn [accum rest-perm]
                      (conj accum
                        (lazy-seq (cons head-val rest-perm))))
              []
              (permute-lazy-1 remaining-vals))))))))

(s/defn permute-nest-1 :- ts/TupleList
  [values]
  (let [output-chan       (chan 999) ; "arbitrary" size output buffer
        permute-nest*     (fn permute-nest*
                            [heads tails]
                            (if (empty? tails)
                              (tas/put-now! output-chan heads)
                              (doseq [ii (range (count tails))]
                                (let [curr-val   (nth tails ii)
                                      next-heads (append heads curr-val)
                                      next-tails (drop-at tails ii) ]
                                  (permute-nest* next-heads next-tails)))))

        gather-results    (fn gather-results []
                            (lazy-seq
                              (when-let [curr-val (tas/take-now! output-chan) ]
                                (cons curr-val (gather-results)))))
        ]
    ; Start generating solutions; will block when channel fills
    (-> (fn []
          (permute-nest* [] values) ; will place results on output-chan
          (close! output-chan))       ; indicates no more results coming
      (Thread.)
      (.start))
    ; Return a lazy sequence of the results
    (gather-results)))

(s/defn permute-nest-2 :- ts/TupleList
  [values]
  (let [output-chan       (chan 999) ; "arbitrary" size output buffer
        permute-nest*     (fn permute-nest*
                            [heads tails]
                            (if (empty? tails)
                              (tas/put-now! output-chan heads)
                              (doseq [ii (range (count tails))]
                                (let [curr-val   (nth tails ii)
                                      next-heads (append heads curr-val)
                                      next-tails (drop-at tails ii) ]
                                  (permute-nest* next-heads next-tails)))))

        gather-results    (fn gather-results []
                            (lazy-seq
                              (when-let [curr-val (tas/take-now! output-chan) ]
                                (cons curr-val (gather-results)))))

        threads-active    (atom (count values))
  ]
    ; Start a thread for each original value
    (doseq [ii (range (count values)) ]
      (let [curr-val   (nth values ii)
            init-heads [curr-val]
            init-tails (drop-at values ii) ]
        ; Start generating solutions; will block when channel fills
        (-> (fn []
            ; (println " starting init-heads=" init-heads "  init-tails=" init-tails)
              (permute-nest* init-heads init-tails) ; will place results on output-chan
              (swap! threads-active dec)
              (when (zero? @threads-active) ; last worker to finish must close channel
                ; sync not needed since multiple close! is idempotent
                (close! output-chan))) ; indicates no more results coming
            (Thread.)
            (.start))))

    ; Return a lazy sequence of the results
    (gather-results)))

(s/defn permute :- ts/TupleList
  "Given a vector of values, return a set of all possible permutations.

      ***** WARNING: number of returned Vectors is (factorial (count values)) *****
  "
  [values :- ts/List]
  (when (empty? values)
    (throw (IllegalArgumentException.
             (str "permute: cannot permute empty set: " values))))
  (permute-nest-2 values))

(comment

  ; Create a finite-length lazy seq
  (defn finite-lazy-builder [values]
    (lazy-seq
      (when-let [ss (seq values)]
        (cons (first values)
              (finite-lazy-builder (next values))))))
  (println (finite-lazy-builder [1 2 3 4 5] ))

  (->
    (fn []
      (doseq [ii (range 3)]
        (println ii)
        (Thread/sleep 999))
      (println "done"))
    (Thread.)
    (.start))

  )
