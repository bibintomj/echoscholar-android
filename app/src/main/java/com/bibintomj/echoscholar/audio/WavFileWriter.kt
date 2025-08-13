package com.bibintomj.echoscholar.audio

import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

class WavFileWriter(private val outFile: File) {
    private var fos: FileOutputStream = FileOutputStream(outFile)
    private var totalPcmBytes: Long = 0
    private val sampleRate = 16000 // match your AudioRecord
    private val channels = 1
    private val bitsPerSample = 16

    init { fos.write(ByteArray(44)) } // reserve header
    fun writePcm(pcm: ByteArray, read: Int) { fos.write(pcm); totalPcmBytes += pcm.size }
    fun finish() {
        fos.flush(); fos.fd.sync(); fos.close()
        val totalDataLen = totalPcmBytes + 36
        val byteRate = sampleRate * channels * bitsPerSample / 8
        RandomAccessFile(outFile, "rw").use { raf ->
            raf.seek(0)
            val b = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            b.put("RIFF".toByteArray())
            b.putInt(totalDataLen.toInt())
            b.put("WAVE".toByteArray())
            b.put("fmt ".toByteArray())
            b.putInt(16); b.putShort(1)
            b.putShort(channels.toShort()); b.putInt(sampleRate)
            b.putInt(byteRate)
            b.putShort((channels * bitsPerSample / 8).toShort())
            b.putShort(bitsPerSample.toShort())
            b.put("data".toByteArray())
            b.putInt(totalPcmBytes.toInt())
            raf.write(b.array())
        }
    }
}
