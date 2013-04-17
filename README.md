# pipe-seq

See http://www.pitheringabout.com/?p=874

## Usage

`(->> (iterate inc 0)
      (take 100)
      (pipe-seq some-fn 10 1)
      doall)`

## Install

:dependencies [[pipe-seq "0.1.0"]]

## License

Distributed under the Eclipse Public License, the same as Clojure.
