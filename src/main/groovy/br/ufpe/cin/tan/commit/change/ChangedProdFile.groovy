package br.ufpe.cin.tan.commit.change

/***
 * Represents a code change in an application file by a commit.
 */
class ChangedProdFile implements CodeChange {

    String path
    def type //add file, remove file, change file, copy file or renaming file
    List<Integer> lines

    @Override
    String toString() {
        "(Application change) path: $path; type: $type; lines: $lines"
    }

}
