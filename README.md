# pipe-seq

Extremely simple parallelism, giving you more control that Clojures pmap.

See http://www.pitheringabout.com/?p=874

## Usage

`(time 
  (->> (iterate inc 0)
      (take 100)
      (pipe-seq (fn [x] (Thread/sleep 1000) x) 10 1)
      doall))`
      
Above takes ten seconds using ten threads with a queue size of one.

feed the pipe in a non blocking manner with a limited queue size:

`(let [[s f] (nonblocking-feed-pipe 5)] (dotimes [n 10] (f n)) (doseq [x s] (println x)))`

## Install

:dependencies [[pipe-seq "0.3.0"]]

## License

Distributed under the Eclipse Public License, the same as Clojure.
