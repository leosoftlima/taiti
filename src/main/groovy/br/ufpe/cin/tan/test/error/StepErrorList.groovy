package br.ufpe.cin.tan.test.error

class StepErrorList {

    String path
    String text
    int size

    @Override
    boolean equals(o) {
        if (o==null) return false
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        StepErrorList that = (StepErrorList) o

        if (size != that.size) return false
        if (path != that.path) return false
        if (text != that.text) return false

        return true
    }

    @Override
    int hashCode() {
        int result
        result = (path != null ? path.hashCode() : 0)
        result = 31 * result + (text != null ? text.hashCode() : 0)
        result = 31 * result + size
        return result
    }

    @Override
    String toString() {
        return "StepErrorList{" +
                "path='" + path + '\'' +
                ", text='" + text + '\'' +
                ", size=" + size +
                '}';
    }
}
