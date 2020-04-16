(ns receive.service.persistence-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [receive.service.persistence :as persistence]))

(defn create-temp-file [file]
  (with-open [file (io/writer file)]
    (.write file "Some data"))
  (io/file file))

(deftest save-file-to-disk
  (let [filename "/tmp/save-to-file-test"
        tempfile (create-temp-file "/tmp/test_tempfile.dat")]
    (persistence/save-to-disk tempfile filename)
    (is (.exists (io/file filename)))))