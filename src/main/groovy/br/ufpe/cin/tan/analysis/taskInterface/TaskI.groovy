package br.ufpe.cin.tan.analysis.taskInterface

import br.ufpe.cin.tan.util.Util


class TaskI extends TaskInterface {

    boolean isEmpty() {
        if (files.empty) true
        else false
    }

    Set<String> getFiles() {
        def candidates = classes.collect { Util.REPOSITORY_FOLDER_PATH + it.file }
        def prodFiles = candidates?.findAll { Util.isApplicationFile(it) }  //only application files (extensions that TestI might reach)
        //def prodFiles = candidates//?.findAll { Util.isApplicationFile(it) }  //all changed files
        if (prodFiles.empty) return []
        def files = prodFiles
        Util.organizePathsForInterfaces(files) as Set
    }

    @Override
    Set<String> findFilteredFiles() {
        Util.filterFiles(files)
    }

}
