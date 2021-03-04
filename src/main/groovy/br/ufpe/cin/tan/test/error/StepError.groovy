package br.ufpe.cin.tan.test.error

class StepError {

    String path
    String text

    @Override
    boolean equals(o) {
        if (o==null) return false
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        StepError stepError = (StepError) o

        if (path != stepError.path) return false
        if (text != stepError.text) return false

        return true
    }

    @Override
    int hashCode() {
        int result
        result = (path != null ? path.hashCode() : 0)
        result = 31 * result + (text != null ? text.hashCode() : 0)
        return result
    }

    @Override
    String toString() {
        return "StepError{" +
                "path='" + path + '\'' +
                ", text='" + text + '\'' +
                '}';
    }
}
