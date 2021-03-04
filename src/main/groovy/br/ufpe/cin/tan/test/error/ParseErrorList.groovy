package br.ufpe.cin.tan.test.error

class ParseErrorList {

    String path
    List<String> msgs

    @Override
    boolean equals(o) {
        if (o==null) return false
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        ParseErrorList that = (ParseErrorList) o

        if (msgs != that.msgs) return false
        if (path != that.path) return false

        return true
    }

    @Override
    int hashCode() {
        int result
        result = (path != null ? path.hashCode() : 0)
        result = 31 * result + (msgs != null ? msgs.hashCode() : 0)
        return result
    }

    @Override
    String toString() {
        return "ParseErrorList{" +
                "path='" + path + '\'' +
                ", msgs=" + msgs +
                '}';
    }
}
