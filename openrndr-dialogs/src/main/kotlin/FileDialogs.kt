package org.openrndr.dialogs

import java.io.File

import org.lwjgl.util.nfd.NativeFileDialog
import org.lwjgl.system.MemoryUtil.*
import org.lwjgl.util.nfd.NFDPathSet
import org.lwjgl.util.nfd.NativeFileDialog.*
import org.openrndr.platform.Platform
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.*

private fun getDefaultPathForContext(programName: String, contextID: String): String? {
    val props = Properties()

    return try {
        val f = File(Platform.supportDirectory(programName), ".file-dialog.properties")
        if (f.exists()) {
            val `is` = FileInputStream(f)
            props.load(`is`)
            `is`.close()
            props.getProperty(contextID, null)
        } else {
            null
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun setDefaultPathForContext(programName: String, contextID: String, file: File) {
    val props = Properties()

    try {
        val f = File(Platform.supportDirectory(programName), ".file-dialog.properties")
        if (!f.exists()) {
            f.createNewFile()
        }
        val `is` = FileInputStream(f)
        props.load(`is`)
        props.setProperty(contextID, file.let { if (it.isDirectory) it.absolutePath else it.parentFile.absolutePath })
        `is`.close()
        val os = FileOutputStream(f)
        props.store(os, "File dialog properties for $programName")
        os.close()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun openFileDialog(programName: String = "OPENRNDR", contextID: String = "global", function: (File) -> Unit) {
    openFileDialog(programName, contextID, emptyList(), function)
}

fun openFileDialog(programName: String = "OPENRNDR", contextID: String = "global", supportedExtensions: List<String>, function: (File) -> Unit) {
    val filterList: CharSequence? = if (supportedExtensions.isEmpty()) null else supportedExtensions.joinToString(";")
    println(filterList)
    val defaultPath: CharSequence? = getDefaultPathForContext(programName, contextID)
    val out = memAllocPointer(1)

    val r = NativeFileDialog.NFD_OpenDialog(filterList, defaultPath, out)
    if (r == NFD_OKAY) {
        val ptr = out.get(0)
        val str = memUTF8(ptr)
        val f = File(str)
        setDefaultPathForContext(programName, contextID, f)
        function(f)
    }
    memFree(out)
}

fun openFilesDialog(programName: String = "OPENRNDR", contextID: String = "global", supportedExtensions: List<String>, function: (List<File>) -> Unit) {
    val filterList: CharSequence? = if (supportedExtensions.isEmpty()) null else supportedExtensions.joinToString(";")
    val defaultPath: CharSequence? = getDefaultPathForContext(programName, contextID)

    val pathSet = NFDPathSet.calloc()

    val r = NativeFileDialog.NFD_OpenDialogMultiple(filterList, defaultPath, pathSet)
    val files = mutableListOf<File>()
    if (r == NFD_OKAY) {
        for (i in 0 until NFD_PathSet_GetCount(pathSet)) {
            val result = NFD_PathSet_GetPath(pathSet, i)
            if (result != null) {
                files.add(File(result))
            }
        }
    }
    NFD_PathSet_Free(pathSet)
    if (files.isNotEmpty()) {
        setDefaultPathForContext(programName, contextID, files[0])
        function(files)
    }
}

fun openFilesDialog(programName: String = "OPENRNDR", contextID: String = "global", function: (List<File>) -> Unit) {
    openFilesDialog(programName, contextID, emptyList(), function)
}

fun openFolderDialog(programName: String = "OPENRNDR", contextID: String = "global", function: (File) -> Unit) {
    val defaultPath: CharSequence? = getDefaultPathForContext(programName, contextID)
    val out = memAllocPointer(1)

    val r = NativeFileDialog.NFD_PickFolder(defaultPath, out)
    if (r == NFD_OKAY) {
        val ptr = out.get(0)
        val str = memUTF8(ptr)
        val f = File(str)
        setDefaultPathForContext(programName, contextID, f)
        function(f)
    }
    memFree(out)
}

fun saveFileDialog(programName: String = "OPENRNDR", contextID: String = "global", function: (File) -> Unit) {
    saveFileDialog(programName, contextID, emptyList(), function)
}

fun saveFileDialog(programName: String = "OPENRNDR", contextID: String = "global", supportedExtensions: List<String>, function: (File) -> Unit) {
    val filterList: CharSequence? = if (supportedExtensions.isEmpty()) null else supportedExtensions.joinToString(";")
    val defaultPath: CharSequence? = getDefaultPathForContext(programName, contextID)
    val out = memAllocPointer(1)
    val r = NativeFileDialog.NFD_SaveDialog(filterList, defaultPath, out)
    if (r == NFD_OKAY) {
        val ptr = out.get(0)
        val str = memUTF8(ptr)
        val f = File(str)
        setDefaultPathForContext(programName, contextID, f)
        function(f)
    }
    memFree(out)
}