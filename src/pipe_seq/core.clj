(ns pipe-seq.core
  (:require [clojure.stacktrace :as st]
            [clojure.tools.logging :as log]))

(def NIL (Object.))
(def EOQ (Object.))

(defn pipe
  "Returns a vector containing a sequence that will read from the
   queue, and a function that inserts items into the queue.

   The default behavour is to block when feeding if the pipe is full.

   The initial size of the queue and the function for feeding should be provided as parameters.
   Copyright Christophe Grand
   See http://clj-me.cgrand.net/2010/04/02/pipe-dreams-are-not-necessarily-made-of-promises/"
  ([size]
     (pipe size (fn [q x] (.put q (or x NIL)))))
  ([size feed-fn]
     (let [q (if size
               (java.util.concurrent.LinkedBlockingQueue. size)
               (java.util.concurrent.LinkedBlockingQueue.))
           s (fn s [] (lazy-seq (let [x (.take q)]
                                  (when-not (= EOQ x)
                                    (cons (when-not (= NIL x) x) (s))))))]
       [(s) (fn ([] (.put q EOQ))
              ([x] (feed-fn q x)))])))

(defn nonblocking-pipe
  "Returns a vector containing a sequence that will read from the
   queue, and a function that inserts items into the queue.

   A variation of the feeder function is provided which feeds the queue
   in a non blocking manner."
  [size]
  (pipe size (fn [q x] (if-not (.offer q (or x NIL))
                       (log/warn x " was rejected")))))

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
        [out-seq out-queue] (pipe pipe-size)]

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
                   (catch Throwable t
                     (log/error t "Error occuring during processing."))
                   (finally
                     (.countDown latch)))))

    ;; Supervisor thread
    (future
      (.await latch)
      (out-queue))

    out-seq))
