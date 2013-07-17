# pipe-seq

Extremely simple parallelism, giving you more control than Clojures pmap.

See http://www.pitheringabout.com/?p=874

## Usage

`(time
  (->> (iterate inc 0)
      (take 100)
      (pipe-seq (fn [x] (Thread/sleep 1000) x) 10 1)
      doall))`

Above takes ten seconds using ten threads with a blocking queue size of one.

Feed the pipe in a non blocking manner with a limited queue size:

`(let [[s f] (nonblocking-pipe 5)] (dotimes [n 10] (f n)) (doseq [x s] (println x)))`

## Dependency

`[pipe-seq "0.3.2"]`

## License

Distributed under the Eclipse Public License, the same as Clojure.
