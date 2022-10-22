package com.wagner.cache_dependencies.util

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.SimpleFileVisitor

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import org.apache.commons.compress.utils.IOUtils

class Archiver implements AutoCloseable {

    private TarArchiveOutputStream tarStream
    private Closure writeCallback

    Archiver(File archiveFile) throws IOException {
        tarStream = new TarArchiveOutputStream(new GzipCompressorOutputStream(archiveFile.newOutputStream()))
    }

    Archiver(File archiveFile, Closure writeCallback) {
        super(archiveFile)
        this.writeCallback = writeCallback
    }

    @Override
    void close() throws IOException {
        tarStream.close()
    }

    void write(File file, String into) throws IOException {
        if(file.isDirectory()) {
            // writeFile(file, into)
            file.eachFile({ child ->
                write(child, Paths.get(into, file.name).toString())
            })
        } else if (file.isFile()) {
            writeFile(file, into)
        } else {
            throw new FileNotFoundException(file)
        }
    }

    void writeFile(File file, String into) throws IOException {
        TarArchiveEntry entry = new TarArchiveEntry(file, into)
        try {
            tarStream.putArchiveEntry(entry)
            tarStream << file.newInputStream()
        } finally {
            tarStream.closeArchiveEntry()
        }
    }
}
