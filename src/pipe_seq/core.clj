(ns pipe-seq.core
  (:require [clojure.stacktrace :as st]
            [clojure.tools.logging :as log]))

(defn pipe
  "Returns a vector containing a sequence that will read from the
   queue, and a function that inserts items into the queue.

   If whatever value provided by the key :feed-non-blocking queue will be fed in a
   non blocking manner using offer insteat of put.
   Copyright Christophe Grand
   See http://clj-me.cgrand.net/2010/04/02/pipe-dreams-are-not-necessarily-made-of-promises/"
  [& {:keys [size feed-non-blocking] :as args}]
  (let [size (:size args)
        fnb (contains?  args :feed-non-blocking)
        q (if size
            (java.util.concurrent.LinkedBlockingQueue. size)
            (java.util.concurrent.LinkedBlockingQueue.))
        EOQ (Object.)
        NIL (Object.)
        feed-fn (if fnb
                  (fn [x] (if-not (.offer q (or x NIL))
                            (log/warn x " was rejected")))
                  (fn [x] (.put q (or x NIL))))
        s (fn s [] (lazy-seq (let [x (.take q)]
                               (when-not (= EOQ x)
                                 (cons (when-not (= NIL x) x) (s))))))]
    [(s) (fn ([] (.put q EOQ))
           ([x] (feed-fn x)))]))

(defn pipe-seq
  "See http://www.pitheringabout.com/?p=874

   Consumes the col with function f returning a new lazy seq.
   The consumption is done in parallel using n-threads backed
   by a queue of the specified size. The output sequence is also
   backed by a queue of the same given size."
  [f n-threads pipe-size col]
  (let [q (java.util.concurrent.LinkedBlockingQueue. pipe-size)
        finished-feeding (promise)
        latch (java.util.concurrent.CountDownLatch. n-threads)
        [out-seq out-queue] (pipe :size pipe-size)]

    ;; Feeder thread
    (future
      (doseq [v (remove nil? col)]
        (.put q v))
      (deliver finished-feeding true))

    (dotimes [i n-threads]
      (future (try (loop []
                     (let [v (.poll q 50 java.util.concurrent.TimeUnit/MILLISECONDS)]
                       (when v (out-queue (f v)))
                       (when-not (and (zero? (.size q))
                                      (realized? finished-feeding))
                         (recur))))
                   (catch Exception e
                     (let [writer (java.io.StringWriter.)]
                       (binding [*out* writer]
                         (st/print-stack-trace e)
                         (log/error (str writer)))))
                   (finally
                     (.countDown latch)))))

    ;; Supervisor thread
    (future
      (.await latch)
      (out-queue))

    out-seq))
