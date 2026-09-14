package hr.rostanic20.gymbro.core

import java.io.File

data class PhotoTarget(val fileName: String, val uri: String)

interface PhotoStorage {
    fun newCaptureTarget(): PhotoTarget
    suspend fun importFrom(sourceUri: String): String
    fun delete(fileName: String)
    fun fileFor(fileName: String): File
}
