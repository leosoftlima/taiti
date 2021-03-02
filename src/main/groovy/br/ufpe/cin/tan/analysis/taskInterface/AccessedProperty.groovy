package br.ufpe.cin.tan.analysis.taskInterface

class AccessedProperty {

    String name
    String type
    String file

    @Override
    boolean equals(o) {
        if (o==null) return false
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        AccessedProperty that = (AccessedProperty) o

        if (file != that.file) return false
        if (name != that.name) return false
        if (type != that.type) return false

        return true
    }

    @Override
    int hashCode() {
        int result
        result = (name != null ? name.hashCode() : 0)
        result = 31 * result + (type != null ? type.hashCode() : 0)
        result = 31 * result + (file != null ? file.hashCode() : 0)
        return result
    }


    @Override
    String toString() {
                "name: '" + name + '\'' +
                ", type: '" + type + '\'' +
                ", file: '" + file
    }
}
