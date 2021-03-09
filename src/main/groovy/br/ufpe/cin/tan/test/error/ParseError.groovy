package br.ufpe.cin.tan.test.error

class ParseError {

    String path
    String msg

    @Override
    boolean equals(o) {
        if (o==null) return false
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        ParseError that = (ParseError) o

        if (msg != that.msg) return false
        if (path != that.path) return false

        return true
    }

    @Override
    int hashCode() {
        int result
        result = (path != null ? path.hashCode() : 0)
        result = 31 * result + (msg != null ? msg.hashCode() : 0)
        return result
    }

    @Override
    String toString() {
        return "ParseError{" +
                "path='" + path + '\'' +
                ", msg='" + msg + '\'' +
                '}';
    }

}
