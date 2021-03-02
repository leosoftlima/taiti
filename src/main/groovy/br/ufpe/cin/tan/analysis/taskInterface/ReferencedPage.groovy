package br.ufpe.cin.tan.analysis.taskInterface

class ReferencedPage {

    String file
    String step

    @Override
    boolean equals(o) {
        if (o==null) return false
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        ReferencedPage that = (ReferencedPage) o

        if (file != that.file) return false
        if (step != that.step) return false

        return true
    }

    @Override
    int hashCode() {
        int result
        result = (file != null ? file.hashCode() : 0)
        result = 31 * result + (step != null ? step.hashCode() : 0)
        return result
    }


    @Override
    String toString() {
        return "file: '" + file + '\'' +
                ", step: '" + step
    }
}
