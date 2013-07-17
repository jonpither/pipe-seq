# pipe-seq

Extremely simple parallelism, giving you more control that Clojures pmap.

See http://www.pitheringabout.com/?p=874

## Usage

`(->> (iterate inc 0)
      (take 100)
      (pipe-seq some-fn 10 1)
      doall)`

feed the pipe in a non blocking manner with a limited queue size:

`(let [[s f] (nonblocking-feed-pipe 5)] (dotimes [n 10] (f n)) (doseq [x s] (println x)))`

## Install

:dependencies [[pipe-seq "0.3.0"]]

## License

Distributed under the Eclipse Public License, the same as Clojure.
