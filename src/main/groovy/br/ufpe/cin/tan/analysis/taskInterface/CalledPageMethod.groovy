package br.ufpe.cin.tan.analysis.taskInterface

class CalledPageMethod {

    String file
    String name
    String args
    String step

    @Override
    boolean equals(o) {
        if (o==null) return false
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        CalledPageMethod that = (CalledPageMethod) o

        if (args != that.args) return false
        if (file != that.file) return false
        if (name != that.name) return false
        if (step != that.step) return false

        return true
    }

    @Override
    int hashCode() {
        int result
        result = (file != null ? file.hashCode() : 0)
        result = 31 * result + (name != null ? name.hashCode() : 0)
        result = 31 * result + (args != null ? args.hashCode() : 0)
        result = 31 * result + (step != null ? step.hashCode() : 0)
        return result
    }


    @Override
    String toString() {
        return "file: '" + file + '\'' +
                ", name: '" + name + '\'' +
                ", args: '" + args + '\'' +
                ", step: '" + step
    }
}
