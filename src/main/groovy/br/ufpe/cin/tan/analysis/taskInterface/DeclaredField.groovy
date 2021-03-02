package br.ufpe.cin.tan.analysis.taskInterface

class DeclaredField {

    String name
    String type
    String value
    String file

    @Override
    boolean equals(o) {
        if (o==null) return false
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        DeclaredField that = (DeclaredField) o

        if (file != that.file) return false
        if (name != that.name) return false
        if (type != that.type) return false
        if (value != that.value) return false

        return true
    }

    @Override
    int hashCode() {
        int result
        result = (name != null ? name.hashCode() : 0)
        result = 31 * result + (type != null ? type.hashCode() : 0)
        result = 31 * result + (value != null ? value.hashCode() : 0)
        result = 31 * result + (file != null ? file.hashCode() : 0)
        return result
    }


    @Override
    String toString() {
        return "name: '" + name + '\'' +
                ", type: '" + type + '\'' +
                ", value: '" + value + '\'' +
                ", file: '" + file
    }
}
