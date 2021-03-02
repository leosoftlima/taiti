package br.ufpe.cin.tan.analysis.taskInterface

class CalledMethod {

    String name
    String type
    String file
    String step

    @Override
    boolean equals(o) {
        if (o==null) return false
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        CalledMethod that = (CalledMethod) o

        if (file != that.file) return false
        if (name != that.name) return false
        if (step != that.step) return false
        if (type != that.type) return false

        return true
    }

    @Override
    int hashCode() {
        int result
        result = (name != null ? name.hashCode() : 0)
        result = 31 * result + (type != null ? type.hashCode() : 0)
        result = 31 * result + (file != null ? file.hashCode() : 0)
        result = 31 * result + (step != null ? step.hashCode() : 0)
        return result
    }


    @Override
    String toString() {
        return "name: '" + name + '\'' +
                ", type: '" + type + '\'' +
                ", file: '" + file + '\'' +
                ", step: '" + step
    }
}
