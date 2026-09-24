package com.harmonyvoice.app

class HarmonyEngine {

    private val sampleRate = 44100L
    private val channels = 1L

    private fun createSoundTouch(): com.tianscar.soundtouch.SoundTouch {
        return com.tianscar.soundtouch.SoundTouch().apply {
            setSampleRate(sampleRate)
            setChannels(channels)
        }
    }

    private fun isSupportedWav(header: ByteArray): Boolean {
        if (header.size < 44) return false

        val riff = String(header, 0, 4, Charsets.US_ASCII)
        val wave = String(header, 8, 4, Charsets.US_ASCII)
        val fmt = String(header, 12, 4, Charsets.US_ASCII)

        if (riff != "RIFF" || wave != "WAVE" || fmt != "fmt ") {
            return false
        }

        val channelsInFile =
            (header[22].toInt() and 0xFF) or
            ((header[23].toInt() and 0xFF) shl 8)

        val sampleRateInFile =
            (header[24].toInt() and 0xFF) or
            ((header[25].toInt() and 0xFF) shl 8) or
            ((header[26].toInt() and 0xFF) shl 16) or
            ((header[27].toInt() and 0xFF) shl 24)

        val bitsPerSample =
            (header[34].toInt() and 0xFF) or
            ((header[35].toInt() and 0xFF) shl 8)

        return channelsInFile == 1 &&
        sampleRateInFile.toLong() == sampleRate &&
        bitsPerSample == 16
    }

    data class VoiceAnalysis(
    val durationMs: Long,
    val sampleCount: Long,
    val rmsLevel: Float,
    val estimatedPitchHz: Float?
)

fun analyzeVoice(
    inputWavPath: String
): VoiceAnalysis? {

    return try {

        val file = java.io.File(inputWavPath)

        if (!file.exists() || file.length() <= 44L) {
            return null
        }

        java.io.FileInputStream(file).use { input ->

            val header = ByteArray(44)

            if (input.read(header) != 44 || !isSupportedWav(header)) {
                return null
            }

            val dataSize = file.length() - 44L
            val sampleCount = dataSize / 2L
            val durationMs =
                (sampleCount * 1000L) / sampleRate

            val buffer = ByteArray(8192)

            var totalEnergy = 0.0
            var totalSamples = 0L

            while (true) {

                val bytesRead = input.read(buffer)

                if (bytesRead <= 0) {
                    break
                }

                val samplesRead = bytesRead / 2

                for (i in 0 until samplesRead) {

                    val low =
                        buffer[i * 2].toInt() and 0xFF

                    val high =
                        buffer[i * 2 + 1].toInt()

                    val sample =
                        ((high shl 8) or low).toShort().toInt()

                    totalEnergy +=
                        sample.toDouble() * sample.toDouble()

                    totalSamples++
                }
            }

            val rmsLevel =
                if (totalSamples > 0) {
                    kotlin.math.sqrt(
                        totalEnergy / totalSamples
                    ).toFloat()
                } else {
                    0f
                }

            VoiceAnalysis(
                durationMs = durationMs,
                sampleCount = sampleCount,
                rmsLevel = rmsLevel,
                estimatedPitchHz = null
            )
        }

    } catch (_: Exception) {

        null
    }
}
    fun createHarmonyVoice(
        inputWavPath: String,
        outputWavPath: String,
        pitchSemiTones: Float
    ): Boolean {
        return try {
            val inputFile = java.io.File(inputWavPath)

            if (!inputFile.exists() || inputFile.length() <= 44L) {
                return false
            }

            val input = java.io.FileInputStream(inputFile)
            val output = java.io.FileOutputStream(outputWavPath)

            val inputHeader = ByteArray(44)
            val headerRead = input.read(inputHeader)

            if (headerRead != 44 || !isSupportedWav(inputHeader)) {
                input.close()
                output.close()
                return false
            }

            // Réserve 44 octets pour le nouvel en-tête WAV.
            output.write(ByteArray(44))

            val soundTouch = createSoundTouch()
            soundTouch.setPitchSemiTones(pitchSemiTones)

            val inputBuffer = ByteArray(8192)
            val shortBuffer = ShortArray(inputBuffer.size / 2)
            val outputBuffer = ShortArray(shortBuffer.size * 2)

            var outputDataSize = 0L

            while (true) {
                val bytesRead = input.read(inputBuffer)

                if (bytesRead <= 0) {
                    break
                }

                val samplesRead = bytesRead / 2

                for (i in 0 until samplesRead) {
                    val low = inputBuffer[i * 2].toInt() and 0xFF
                    val high = inputBuffer[i * 2 + 1].toInt()

                    shortBuffer[i] =
                        ((high shl 8) or low).toShort()
                }

                soundTouch.putSamples(
                    shortBuffer,
                    0,
                    samplesRead
                )

                while (soundTouch.numSamples() > 0) {
                    val received = soundTouch.receiveSamplesI16(
                        outputBuffer,
                        0,
                        outputBuffer.size
                    )

                    if (received <= 0) {
                        break
                    }

                    for (i in 0 until received) {
                        val sample = outputBuffer[i].toInt()

                        output.write(sample and 0xFF)
                        output.write((sample shr 8) and 0xFF)

                        outputDataSize += 2
                    }
                }
            }

            soundTouch.flush()

            while (!soundTouch.isEmpty()) {
                val received = soundTouch.receiveSamplesI16(
                    outputBuffer,
                    0,
                    outputBuffer.size
                )

                if (received <= 0) {
                    break
                }

                for (i in 0 until received) {
                    val sample = outputBuffer[i].toInt()

                    output.write(sample and 0xFF)
                    output.write((sample shr 8) and 0xFF)

                    outputDataSize += 2
                }
            }

            soundTouch.dispose()
            input.close()
            output.close()

            writeWavHeader(
                outputWavPath,
                outputDataSize
            )

            true

        } catch (_: Exception) {
            false
        }
    }

    private fun writeWavHeader(
        wavPath: String,
        dataSize: Long
    ) {
        val fileSize = 36L + dataSize

        val header = ByteArray(44)

        // RIFF
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()

        writeIntLE(header, 4, fileSize.toInt())

        // WAVE
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()

        // fmt
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()

        writeIntLE(header, 16, 16)

        // PCM
        writeShortLE(header, 20, 1)

        // Mono
        writeShortLE(header, 22, 1)

        // 44100 Hz
        writeIntLE(header, 24, sampleRate.toInt())

        // Byte rate
        writeIntLE(
            header,
            28,
            sampleRate.toInt() * 2
        )

        // Block align
        writeShortLE(header, 32, 2)

        // 16 bits
        writeShortLE(header, 34, 16)

        // data
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()

        writeIntLE(header, 40, dataSize.toInt())

        val file = java.io.RandomAccessFile(
            wavPath,
            "rw"
        )

        file.seek(0)
        file.write(header)
        file.close()
    }

    private fun writeIntLE(
        buffer: ByteArray,
        offset: Int,
        value: Int
    ) {
        buffer[offset] = (value and 0xFF).toByte()
        buffer[offset + 1] =
            ((value shr 8) and 0xFF).toByte()
        buffer[offset + 2] =
            ((value shr 16) and 0xFF).toByte()
        buffer[offset + 3] =
            ((value shr 24) and 0xFF).toByte()
    }

    private fun writeShortLE(
        buffer: ByteArray,
        offset: Int,
        value: Int
    ) {
        buffer[offset] = (value and 0xFF).toByte()
        buffer[offset + 1] =
            ((value shr 8) and 0xFF).toByte()
    }

    fun prepareVoice(inputWavPath: String): String {
        return inputWavPath
    }

    fun isReady(): Boolean {
        return true
    }
}
