package com.harmonyvoice.app

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round
import kotlin.math.sqrt

class HarmonyEngine {

    private val sampleRate = 44100L
    private val channels = 1L

    /*
     * ============================================================
     * SOUND TOUCH
     * ============================================================
     */

    private fun createSoundTouch(): com.tianscar.soundtouch.SoundTouch {
        return com.tianscar.soundtouch.SoundTouch().apply {
            setSampleRate(sampleRate)
            setChannels(channels)
        }
    }

    /*
     * ============================================================
     * DATA CLASSES
     * ============================================================
     */

    data class VoiceAnalysis(
        val durationMs: Long,
        val sampleCount: Long,
        val rmsLevel: Float,
        val estimatedPitchHz: Float?,
        val tempoBpm: Float = 0f,
        val keyName: String = "Inconnue",
        val keyRoot: Int = -1,
        val isMinor: Boolean = false,
        val detectedNotes: List<DetectedNote> = emptyList()
    )

    data class DetectedNote(
        val pitchHz: Float,
        val startMs: Long,
        val endMs: Long
    )

    data class HarmonyAnalysis(
        val tempoBpm: Float,
        val keyName: String,
        val keyRoot: Int,
        val isMinor: Boolean,
        val notes: List<DetectedNote>,
        val sopranoShifts: List<Float>,
        val altoShifts: List<Float>,
        val tenorShifts: List<Float>
    )

    data class HarmonyResult(
        val analysis: HarmonyAnalysis,
        val sopranoPath: String?,
        val altoPath: String?,
        val tenorPath: String?
    )

    /*
     * ============================================================
     * WAV
     * ============================================================
     */

    private fun isSupportedWav(header: ByteArray): Boolean {

        if (header.size < 44) return false

        val riff = String(
            header,
            0,
            4,
            Charsets.US_ASCII
        )

        val wave = String(
            header,
            8,
            4,
            Charsets.US_ASCII
        )

        val fmt = String(
            header,
            12,
            4,
            Charsets.US_ASCII
        )

        if (
            riff != "RIFF" ||
            wave != "WAVE" ||
            fmt != "fmt "
        ) {
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

        return (
                channelsInFile == 1 &&
                        sampleRateInFile.toLong() == sampleRate &&
                        bitsPerSample == 16
                )
    }

    /*
     * ============================================================
     * ANALYSE COMPLETE
     * ============================================================
     */

    fun analyzeVoice(
        inputWavPath: String
    ): VoiceAnalysis? {

        return try {

            val file = File(inputWavPath)

            if (
                !file.exists() ||
                file.length() <= 44L
            ) {
                return null
            }

            FileInputStream(file).use { input ->

                val header = ByteArray(44)

                if (
                    input.read(header) != 44 ||
                    !isSupportedWav(header)
                ) {
                    return null
                }

                val dataSize =
                    file.length() - 44L

                val sampleCount =
                    dataSize / 2L

                val durationMs =
                    (sampleCount * 1000L) / sampleRate

                val rmsLevel =
                    calculateRms(file)

                val detectedNotes =
                    detectNotes(inputWavPath)

                val estimatedPitchHz =
                    if (detectedNotes.isNotEmpty()) {
                        detectedNotes
                            .map { it.pitchHz }
                            .average()
                            .toFloat()
                    } else {
                        null
                    }

                val tempoBpm =
                    detectTempoBpm(inputWavPath)

                val key =
                    detectKey(detectedNotes)

                VoiceAnalysis(
                    durationMs = durationMs,
                    sampleCount = sampleCount,
                    rmsLevel = rmsLevel,
                    estimatedPitchHz = estimatedPitchHz,
                    tempoBpm = tempoBpm,
                    keyName = key.name,
                    keyRoot = key.root,
                    isMinor = key.isMinor,
                    detectedNotes = detectedNotes
                )
            }

        } catch (_: Exception) {

            null
        }
    }

    /*
     * ============================================================
     * RMS
     * ============================================================
     */

    private fun calculateRms(
        file: File
    ): Float {

        var totalEnergy = 0.0
        var totalSamples = 0L

        FileInputStream(file).use { input ->

            val header = ByteArray(44)

            if (input.read(header) != 44) {
                return 0f
            }

            val buffer = ByteArray(8192)

            while (true) {

                val bytesRead =
                    input.read(buffer)

                if (bytesRead <= 0) {
                    break
                }

                val samplesRead =
                    bytesRead / 2

                for (i in 0 until samplesRead) {

                    val low =
                        buffer[i * 2].toInt() and 0xFF

                    val high =
                        buffer[i * 2 + 1].toInt()

                    val sample =
                        ((high shl 8) or low)
                            .toShort()
                            .toInt()

                    totalEnergy +=
                        sample.toDouble() * sample.toDouble()

                    totalSamples++
                }
            }
        }

        if (totalSamples == 0L) {
            return 0f
        }

        return sqrt(
            totalEnergy / totalSamples
        ).toFloat()
    }

    /*
     * ============================================================
     * NOTE DETECTION
     * ============================================================
     */

    fun detectNotes(
        inputWavPath: String
    ): List<DetectedNote> {

        val detectedNotes =
            mutableListOf<DetectedNote>()

        return try {

            val file =
                File(inputWavPath)

            if (
                !file.exists() ||
                file.length() <= 44L
            ) {
                return detectedNotes
            }

            FileInputStream(file).use { input ->

                val header =
                    ByteArray(44)

                if (
                    input.read(header) != 44 ||
                    !isSupportedWav(header)
                ) {
                    return detectedNotes
                }

                val windowSize = 2048
                val hopSize = 1024

                val window =
                    ShortArray(windowSize)

                var samplesInWindow = 0

                while (
                    samplesInWindow < windowSize
                ) {

                    val low =
                        input.read()

                    if (low == -1) break

                    val high =
                        input.read()

                    if (high == -1) break

                    window[samplesInWindow] =
                        ((high shl 8) or
                                (low and 0xFF))
                            .toShort()

                    samplesInWindow++
                }

                var samplePosition = 0L

                while (
                    samplesInWindow == windowSize
                ) {

                    val pitchHz =
                        detectPitchFromWindow(
                            window,
                            0,
                            windowSize
                        )

                    if (pitchHz != null) {

                        val startMs =
                            (samplePosition * 1000L) /
                                    sampleRate

                        val endMs =
                            (
                                    (samplePosition +
                                            windowSize) *
                                            1000L
                                    ) / sampleRate

                        detectedNotes.add(
                            DetectedNote(
                                pitchHz =
                                    pitchHz,
                                startMs =
                                    startMs,
                                endMs =
                                    endMs
                            )
                        )
                    }

                    System.arraycopy(
                        window,
                        hopSize,
                        window,
                        0,
                        windowSize - hopSize
                    )

                    samplesInWindow =
                        windowSize - hopSize

                    while (
                        samplesInWindow < windowSize
                    ) {

                        val low =
                            input.read()

                        if (low == -1) break

                        val high =
                            input.read()

                        if (high == -1) break

                        window[samplesInWindow] =
                            ((high shl 8) or
                                    (low and 0xFF))
                                .toShort()

                        samplesInWindow++
                    }

                    samplePosition += hopSize
                }
            }

            smoothDetectedNotes(
                detectedNotes
            )

        } catch (_: Exception) {

            emptyList()
        }
    }

    /*
     * ============================================================
     * NETTOYAGE DES NOTES
     * ============================================================
     */

    private fun smoothDetectedNotes(
    notes: List<DetectedNote>
): List<DetectedNote> {

    if (notes.isEmpty()) {
        return emptyList()
    }

    val result = mutableListOf<DetectedNote>()

    var currentStart = notes.first().startMs
    var currentEnd = notes.first().endMs
    var pitchSum = notes.first().pitchHz.toDouble()
    var pitchCount = 1

    var currentMidi =
        round(
            hzToMidi(
                notes.first().pitchHz
            )
        ).toInt()

    for (index in 1 until notes.size) {

        val note = notes[index]

        val noteMidi =
            round(
                hzToMidi(
                    note.pitchHz
                )
            ).toInt()

        val difference =
            abs(noteMidi - currentMidi)

        val isContinuous =
            note.startMs <= currentEnd + 40L

        /*
         * Une différence de 0 ou 1 demi-ton
         * peut appartenir à la même note chantée.
         *
         * Au-delà, nous considérons qu'il s'agit
         * d'un véritable changement de note.
         */
        if (
            difference <= 1 &&
            isContinuous
        ) {

            currentEnd =
                note.endMs

            pitchSum +=
                note.pitchHz.toDouble()

            pitchCount++

            currentMidi =
                round(
                    pitchSum /
                            pitchCount
                ).toInt()

        } else {

            result.add(
                DetectedNote(
                    pitchHz =
                        (
                            pitchSum /
                                    pitchCount
                            ).toFloat(),

                    startMs =
                        currentStart,

                    endMs =
                        currentEnd
                )
            )

            currentStart =
                note.startMs

            currentEnd =
                note.endMs

            pitchSum =
                note.pitchHz.toDouble()

            pitchCount = 1

            currentMidi =
                noteMidi
        }
    }

    /*
     * Ajouter la dernière note.
     */
    result.add(
        DetectedNote(
            pitchHz =
                (
                    pitchSum /
                            pitchCount
                    ).toFloat(),

            startMs =
                currentStart,

            endMs =
                currentEnd
        )
    )

    return result
    }
    
    /*
     * ============================================================
     * PITCH
     * ============================================================
     */

    private fun detectPitchFromWindow(
        samples: ShortArray,
        start: Int,
        size: Int
    ): Float? {

        var energy = 0.0

        for (i in 0 until size) {

            val value =
                samples[start + i].toDouble()

            energy += value * value
        }

        if (energy < 1_000_000.0) {
            return null
        }

        val minFrequency = 70.0
        val maxFrequency = 1000.0

        val minLag =
            (sampleRate / maxFrequency).toInt()

        val maxLag =
            min(
                (sampleRate / minFrequency).toInt(),
                size - 2
            )

        var bestLag = 0
        var bestCorrelation = 0.0

        for (lag in minLag..maxLag) {

            var correlation = 0.0
            var energyA = 0.0
            var energyB = 0.0

            for (
                i in 0 until (size - lag)
            ) {

                val a =
                    samples[start + i].toDouble()

                val b =
                    samples[start + i + lag]
                        .toDouble()

                correlation += a * b

                energyA += a * a
                energyB += b * b
            }

            val denominator =
                sqrt(
                    energyA * energyB
                )

            if (denominator > 0.0) {

                val normalizedCorrelation =
                    correlation / denominator

                if (
                    normalizedCorrelation >
                    bestCorrelation
                ) {

                    bestCorrelation =
                        normalizedCorrelation

                    bestLag = lag
                }
            }
        }

        if (
            bestLag <= 0 ||
            bestCorrelation < 0.70
        ) {
            return null
        }

        return sampleRate.toFloat() /
                bestLag.toFloat()
    }

    /*
     * ============================================================
     * TEMPO / BPM
     * ============================================================
     */

    fun detectTempoBpm(
        inputWavPath: String
    ): Float {

        return try {

            val energies =
                readEnergyEnvelope(
                    inputWavPath
                )

            if (energies.size < 20) {
                return 0f
            }

            val hopMs =
                1024.0 * 1000.0 /
                        sampleRate

            val minBpm = 60
            val maxBpm = 180

            var bestBpm = 0
            var bestScore = Double.MIN_VALUE

            for (bpm in minBpm..maxBpm) {

                val beatMs =
                    60000.0 / bpm.toDouble()

                val lag =
                    round(
                        beatMs / hopMs
                    ).toInt()

                if (
                    lag <= 0 ||
                    lag >= energies.size
                ) {
                    continue
                }

                var score = 0.0

                var i = lag

                while (i < energies.size) {

                    score +=
                        energies[i] *
                                energies[i - lag]

                    i += lag
                }

                if (score > bestScore) {

                    bestScore = score
                    bestBpm = bpm
                }
            }

            if (bestBpm == 0) {
                0f
            } else {
                bestBpm.toFloat()
            }

        } catch (_: Exception) {

            0f
        }
    }

    private fun readEnergyEnvelope(
        inputWavPath: String
    ): List<Double> {

        val result =
            mutableListOf<Double>()

        val file =
            File(inputWavPath)

        FileInputStream(file).use { input ->

            val header =
                ByteArray(44)

            if (
                input.read(header) != 44 ||
                !isSupportedWav(header)
            ) {
                return result
            }

            val windowSize = 1024
            val buffer =
                ByteArray(windowSize * 2)

            while (true) {

                val bytesRead =
                    input.read(buffer)

                if (bytesRead <= 0) {
                    break
                }

                val samples =
                    bytesRead / 2

                if (samples == 0) {
                    continue
                }

                var energy = 0.0

                for (i in 0 until samples) {

                    val low =
                        buffer[i * 2]
                            .toInt() and 0xFF

                    val high =
                        buffer[i * 2 + 1]
                            .toInt()

                    val sample =
                        ((high shl 8) or low)
                            .toShort()
                            .toInt()

                    energy +=
                        abs(sample.toDouble())
                }

                result.add(
                    energy / samples
                )
            }
        }

        if (result.isEmpty()) {
            return result
        }

        val average =
            result.average()

        return result.map { value ->

            max(
                0.0,
                value - average
            )
        }
    }

    /*
     * ============================================================
     * TONALITÉ
     * ============================================================
     */

    private data class KeyResult(
        val name: String,
        val root: Int,
        val isMinor: Boolean
    )

    private val noteNames =
        arrayOf(
            "Do",
            "Do#",
            "Ré",
            "Ré#",
            "Mi",
            "Fa",
            "Fa#",
            "Sol",
            "Sol#",
            "La",
            "La#",
            "Si"
        )

    private val majorProfile =
        doubleArrayOf(
            6.35,
            2.23,
            3.48,
            2.33,
            4.38,
            4.09,
            2.52,
            5.19,
            2.39,
            3.66,
            2.29,
            2.88
        )

    private val minorProfile =
        doubleArrayOf(
            6.33,
            2.68,
            3.52,
            5.38,
            2.60,
            3.53,
            2.54,
            4.75,
            3.98,
            2.69,
            3.34,
            3.17
        )

    private fun detectKey(
        notes: List<DetectedNote>
    ): KeyResult {

        if (notes.isEmpty()) {

            return KeyResult(
                name = "Inconnue",
                root = -1,
                isMinor = false
            )
        }

        val histogram =
            DoubleArray(12)

        for (note in notes) {

            val midi =
                hzToMidi(note.pitchHz)

            val pitchClass =
                ((round(midi).toInt() % 12) + 12) % 12

            val duration =
                max(
                    1L,
                    note.endMs - note.startMs
                )

            histogram[pitchClass] +=
                duration.toDouble()
        }

        var bestScore =
                      Double.NEGATIVE_INFINITY

        var bestRoot = 0
        var bestMinor = false

        for (root in 0 until 12) {

            val majorScore =
                profileScore(
                    histogram,
                    majorProfile,
                    root
                )

            if (majorScore > bestScore) {

                bestScore = majorScore
                bestRoot = root
                bestMinor = false
            }

            val minorScore =
                profileScore(
                    histogram,
                    minorProfile,
                    root
                )

            if (minorScore > bestScore) {

                bestScore = minorScore
                bestRoot = root
                bestMinor = true
            }
        }

        val suffix =
            if (bestMinor) {
                " mineur"
            } else {
                " majeur"
            }

        return KeyResult(
            name =
                noteNames[bestRoot] + suffix,

            root =
                bestRoot,

            isMinor =
                bestMinor
        )
    }

    private fun profileScore(
        histogram: DoubleArray,
        profile: DoubleArray,
        root: Int
    ): Double {

        var meanHistogram =
            histogram.average()

        var meanProfile =
            profile.average()

        var numerator = 0.0
        var denominatorA = 0.0
        var denominatorB = 0.0

        for (i in 0 until 12) {

            val a =
                histogram[
                    (i + root) % 12
                ] - meanHistogram

            val b =
           profile[i] - meanProfile

            numerator += a * b
            denominatorA += a * a
            denominatorB += b * b
        }

        val denominator =
            sqrt(
                denominatorA *
                        denominatorB
            )

        if (denominator == 0.0) {
            return 0.0
        }

        return numerator / denominator
    }

    /*
     * ============================================================
     * MIDI
     * ============================================================
     */

    private fun hzToMidi(
        hz: Float
    ): Double {

        if (hz <= 0f) {
            return 0.0
        }

        return 69.0 +
                12.0 *
                kotlin.math.log(
                    hz.toDouble() / 440.0,
                    2.0
                )
    }

    private fun midiToHz(
        midi: Double
    ): Float {

        return (
                440.0 *
                        Math.pow(
                            2.0,
                            (midi - 69.0) / 12.0
                        )
                ).toFloat()
    }

    /*
     * ============================================================
     * CALCUL DU DÉCALAGE MUSICAL
     * ============================================================
     */

    private val majorScale =
        intArrayOf(
            0,
            2,
            4,
            5,
            7,
            9,
            11
        )

    private val minorScale =
        intArrayOf(
            0,
            2,
            3,
            5,
            7,
            8,
            10
        )
        private fun nearestScaleDegree(
        pitchClass: Int,
        keyRoot: Int,
        isMinor: Boolean
    ): Int {

        val scale =
            if (isMinor) {
                minorScale
            } else {
                majorScale
            }

        var bestDegree = 0
        var bestDistance = Int.MAX_VALUE

        for (degree in scale.indices) {

            val scalePitch =
                (keyRoot + scale[degree]) % 12

            val distance =
                min(
                    abs(pitchClass - scalePitch),
                    12 - abs(
                        pitchClass - scalePitch
                    )
                )

            if (distance < bestDistance) {

                bestDistance = distance
                bestDegree = degree
            }
        }

        return bestDegree
    }

    private fun harmonyMidi(
        midi: Double,
        keyRoot: Int,
        isMinor: Boolean,
        degreeOffset: Int
    ): Double {

        if (keyRoot < 0) {
            return midi + degreeOffset
        }

        val roundedMidi =
            round(midi).toInt()

        val pitchClass =
            ((roundedMidi % 12) + 12) % 12

        val scale =
            if (isMinor) {
                minorScale
            } else {
                majorScale
            }

        val degree =
            nearestScaleDegree(
                pitchClass,
                keyRoot,
                isMinor
            )

        var targetDegree =
            degree + degreeOffset

        var octaveShift = 0

        while (targetDegree < 0) {

            targetDegree +=
                scale.size

            octaveShift--
        }

        while (
            targetDegree >= scale.size
        ) {

            targetDegree -=
                scale.size

            octaveShift++
        }

        val currentScalePitch =
            keyRoot + scale[degree]
        val targetScalePitch =
            keyRoot +
                    scale[targetDegree] +
                    12 * octaveShift

        var currentPitch =
            roundedMidi

        while (
            currentPitch -
            currentScalePitch >
            6
        ) {
            currentPitch -= 12
        }

        while (
            currentScalePitch -
            currentPitch >
            6
        ) {
            currentPitch += 12
        }

        val result =
            currentPitch +
                    (
                            targetScalePitch -
                                    currentScalePitch
                            )

        return result.toDouble()
    }

    private fun calculateHarmonyShift(
        pitchHz: Float,
        keyRoot: Int,
        isMinor: Boolean,
        degreeOffset: Int
    ): Float {

        val originalMidi =
            hzToMidi(pitchHz)

        val targetMidi =
            harmonyMidi(
                midi = originalMidi,
                keyRoot = keyRoot,
                isMinor = isMinor,
                degreeOffset = degreeOffset
            )

        return (
                targetMidi -
                        originalMidi
                ).toFloat()
    }

    /*
     * 
     ============================================================
     * ANALYSE HARMONIQUE COMPLÈTE
     * ============================================================
     */

    fun analyzeHarmony(
        inputWavPath: String
    ): HarmonyAnalysis? {

        val analysis =
            analyzeVoice(inputWavPath)
                ?: return null

        val notes =
            analysis.detectedNotes

        if (notes.isEmpty()) {
            return null
        }

        val soprano =
            notes.map {

                calculateHarmonyShift(
                    pitchHz = it.pitchHz,
                    keyRoot = analysis.keyRoot,
                    isMinor = analysis.isMinor,
                    degreeOffset = 0
                )
            }

        val alto =
            notes.map {

                calculateHarmonyShift(
                    pitchHz = it.pitchHz,
                    keyRoot = analysis.keyRoot,
                    isMinor = analysis.isMinor,
                    degreeOffset = -2
                )
            }

        val tenor =
            notes.map {

                calculateHarmonyShift(
                    pitchHz = it.pitchHz,
                    keyRoot = analysis.keyRoot,
                    isMinor = analysis.isMinor,
                    degreeOffset = -4
                )
            }

        return HarmonyAnalysis(
            tempoBpm =
                analysis.tempoBpm,

            keyName =
                analysis.keyName,

            keyRoot =
                analysis.keyRoot,

            isMinor =
                analysis.isMinor,

            notes =
                notes,

            sopranoShifts =
                soprano,

            altoShifts =
                alto,

            tenorShifts =
                tenor
        )
    }
    /*
     * ============================================================
     * GÉNÉRATION D'UNE VOIX
     *
     * Cette méthode existait déjà dans le projet.
     * Elle est conservée pour ne pas casser MainActivity.
     * ============================================================
     */

    fun createHarmonyVoice(
        inputWavPath: String,
        outputWavPath: String,
        pitchSemiTones: Float
    ): Boolean {

        return try {

            val inputFile =
                File(inputWavPath)

            if (
                !inputFile.exists() ||
                inputFile.length() <= 44L
            ) {
                return false
            }

            val input =
                FileInputStream(inputFile)

            val output =
                FileOutputStream(
                    outputWavPath
                )

            val inputHeader =
                ByteArray(44)

            val headerRead =
                input.read(inputHeader)

            if (
                headerRead != 44 ||
                !isSupportedWav(inputHeader)
            ) {

                input.close()
                output.close()

                return false
            }

            /*
             * On réserve la place du header.
             */
            output.write(
                ByteArray(44)
            )

            val soundTouch =
                createSoundTouch()

            /*
             * Très important :
             *
             * setPitchSemiTones()
             * modifie la hauteur de la même voix.
             *
             * Nous ne remplaçons donc pas la voix
             * de l'utilisateur par une voix synthétique.
             */
            soundTouch.setPitchSemiTones(
                pitchSemiTones
            )

            val inputBuffer =
                ByteArray(8192)

            val shortBuffer =
                ShortArray(
                    inputBuffer.size / 2
                )

            val outputBuffer =
                ShortArray(
                    shortBuffer.size * 2
                )

            var outputDataSize = 0L

            while (true) {

                val bytesRead =
                    input.read(inputBuffer)

                if (bytesRead <= 0) {
                    break
                }

                val samplesRead =
                    bytesRead / 2

                for (
                    i in 0 until samplesRead
                ) {

                    val low =
                        inputBuffer[
                            i * 2
                        ].toInt() and 0xFF

                    val high =
                        inputBuffer[
                            i * 2 + 1
                        ].toInt()

                    shortBuffer[i] =
                        ((high shl 8) or low)
                            .toShort()
                }

                soundTouch.putSamples(
                    shortBuffer,
                    0,
                    samplesRead
                )

                while (
                    soundTouch.numSamples() > 0
                ) {

                    val received =
                        soundTouch.receiveSamplesI16(
                            outputBuffer,
                            0,
                            outputBuffer.size
                        )

                    if (received <= 0) {
                        break
                    }

                    for (
                        i in 0 until received
                    ) {

                        val sample =
                            outputBuffer[i]
                                .toInt()

                        output.write(
                            sample and 0xFF
                        )

                        output.write(
                            (sample shr 8) and 0xFF
                        )

                        outputDataSize += 2
                    }
                }
            }
            soundTouch.flush()

            while (
                !soundTouch.isEmpty()
            ) {

                val received =
                    soundTouch.receiveSamplesI16(
                        outputBuffer,
                        0,
                        outputBuffer.size
                    )

                if (received <= 0) {
                    break
                }

                for (
                    i in 0 until received
                ) {

                    val sample =
                        outputBuffer[i]
                            .toInt()

                    output.write(
                        sample and 0xFF
                    )

                    output.write(
                        (sample shr 8) and 0xFF
                    )

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

    /*
     * ============================================================
     * GÉNÉRATION DES 3 PARTIES
     *
     * Cette fonction prépare les décalages musicaux.
     *
     * Soprano = voix originale
     * Alto    = -2 degrés de gamme
     * Ténor   = -4 degrés de gamme
     *
     * Le BPM n'est PAS modifié.
     * ============================================================
     */

    fun createHarmonyPlan(
        inputWavPath: String
    ): HarmonyAnalysis? {

        return analyzeHarmony(
            inputWavPath
        )
    }

    /*
     * ============================================================
     * CRÉATION DES FICHIERS SOPRANO / ALTO / TÉNOR
     *
     * Pour l'instant, le Soprano est une copie de la voix
     * utilisateur.
     *
     * Alto et Ténor utilisent la même source vocale.
     *
     * Le moteur choisit un décalage moyen musical pour chaque
     * partie afin de rester compatible avec SoundTouch actuel.
     *
     * La synchronisation temporelle originale est conservée.
     * ============================================================
     */
     data class HarmonySegment(
    val startMs: Long,
    val endMs: Long,
    val pitchShift: Float
)

private fun medianPitchShift(
    segments: List<HarmonySegment>
): Float {

    if (segments.isEmpty()) {
        return 0f
    }

    val values =
        segments
            .map { it.pitchShift.toDouble() }
            .sorted()

    val middle =
        values.size / 2

    return if (values.size % 2 == 0) {

        (
            values[middle - 1] +
                    values[middle]
        ).toFloat() / 2f

    } else {

        values[middle].toFloat()
    }
}
     fun createHarmonyParts(
    inputWavPath: String,
    sopranoWavPath: String,
    altoWavPath: String,
    tenorWavPath: String
): HarmonyResult? {

    val analysis =
        analyzeHarmony(inputWavPath)
            ?: return null

    val notes = analysis.notes

    if (notes.isEmpty()) {
        return null
    }

    /*
     * ------------------------------------------------------------
     * SOPRANO
     * ------------------------------------------------------------
     *
     * Le soprano conserve exactement l'enregistrement original.
     */
    try {

        FileInputStream(
            inputWavPath
        ).use { input ->

            FileOutputStream(
                sopranoWavPath
            ).use { output ->

                input.copyTo(output)
            }
        }

    } catch (_: Exception) {

        return null
    }

    /*
     * ------------------------------------------------------------
     * ALTO
     * ------------------------------------------------------------
     *
     * Nous construisons ici le plan musical note par note.
     *
     * Chaque note conserve :
     * - son début ;
     * - sa fin ;
     * - son tempo ;
     * - sa position dans la chanson.
     */
    val altoPlan =
        analysis.altoShifts.mapIndexed { index, shift ->

            HarmonySegment(
                startMs =
                    notes[index].startMs,

                endMs =
                    notes[index].endMs,

                pitchShift =
                    shift
            )
        }

    /*
     * ------------------------------------------------------------
     * TÉNOR
     * ------------------------------------------------------------
     */
    val tenorPlan =
        analysis.tenorShifts.mapIndexed { index, shift ->

            HarmonySegment(
                startMs =
                    notes[index].startMs,

                endMs =
                    notes[index].endMs,

                pitchShift =
                    shift
            )
        }

    /*
     * Pour cette étape, nous utilisons encore
     * la transformation SoundTouch existante.
     *
     * La prochaine étape remplacera cette partie par
     * une véritable transformation segmentée.
     *
     * IMPORTANT :
     * nous choisissons le décalage médian plutôt que
     * la moyenne afin d'éviter qu'une note extrême
     * déforme toute la partie.
     */
    val altoShift =
        medianPitchShift(altoPlan)

    val tenorShift =
        medianPitchShift(tenorPlan)

    val altoOk =
        createHarmonyVoice(
            inputWavPath =
                inputWavPath,

            outputWavPath =
                altoWavPath,

            pitchSemiTones =
                altoShift
        )

    if (!altoOk) {
        return null
    }

    val tenorOk =
        createHarmonyVoice(
            inputWavPath =
                inputWavPath,

            outputWavPath =
                tenorWavPath,

            pitchSemiTones =
                tenorShift
        )

    if (!tenorOk) {
        return null
    }

    return HarmonyResult(

        analysis =
            analysis,

        sopranoPath =
            sopranoWavPath,

        altoPath =
            altoWavPath,

        tenorPath =
            tenorWavPath
    )
     }
     
    /*
     * ============================================================
     * MIXAGE
     *
     * Mélange trois fichiers WAV mono 16 bits.
     *
     * Soprano = 1.0
     * Alto    = 0.72
     * Ténor   = 0.62
     *
     * Ces niveaux évitent que les harmonies couvrent totalement
     * la voix principale.
     * ============================================================
     */

    fun mixHarmony(
        sopranoPath: String,
        altoPath: String,
        tenorPath: String,
        outputPath: String
    ): Boolean {

        return try {

            val soprano =
                readWavSamples(
                    sopranoPath
                )

            val alto =
                readWavSamples(
                    altoPath
                )

            val tenor =
                readWavSamples(
                    tenorPath
                )

            if (
                soprano == null ||
                alto == null ||
                tenor == null
            ) {
                return false
            }

            val length =
                max(
                    soprano.size,
                    max(
                        alto.size,
                        tenor.size
                    )
                )

            val mixed =
                ShortArray(length)

            for (i in 0 until length) {

                val s =
                    if (i < soprano.size) {
                        soprano[i]
                            .toDouble()
                    } else {
                        0.0
                    }
                    val a =
                    if (i < alto.size) {
                        alto[i]
                            .toDouble() *
                                0.72
                    } else {
                        0.0
                    }

                val t =
                    if (i < tenor.size) {
                        tenor[i]
                            .toDouble() *
                                0.62
                    } else {
                        0.0
                    }

                val value =
                    s + a + t

                mixed[i] =
                    value
                        .coerceIn(
                            -32768.0,
                            32767.0
                        )
                        .toInt()
                        .toShort()
            }

            writeWavSamples(
                outputPath,
                mixed
            )

            true

        } catch (_: Exception) {

            false
        }
    }

    /*
     * ============================================================
     * LECTURE WAV
     * ============================================================
     */

    private fun readWavSamples(
        path: String
    ): ShortArray? {

        val file =
            File(path)

        if (
            !file.exists() ||
            file.length() <= 44L
        ) {
            return null
        }

        FileInputStream(file).use { input ->

            val header =
                ByteArray(44)

            if (
                input.read(header) != 44 ||
                !isSupportedWav(header)
            ) {
                return null
            }

            val dataSize =
                file.length() - 44L

            val sampleCount =
                (dataSize / 2L)
                    .toInt()

            val samples =
                ShortArray(sampleCount)

            val buffer =
                ByteArray(8192)

            var position = 0

            while (
                position < sampleCount
            ) {

   val bytesRead =
                    input.read(buffer)

                if (bytesRead <= 0) {
                    break
                }

                val samplesRead =
                    min(
                        bytesRead / 2,
                        sampleCount - position
                    )

                for (
                    i in 0 until samplesRead
                ) {

                    val low =
                        buffer[
                            i * 2
                        ].toInt() and 0xFF

                    val high =
                        buffer[
                            i * 2 + 1
                        ].toInt()

                    samples[position + i] =
                        ((high shl 8) or low)
                            .toShort()
                }

                position += samplesRead
            }

            return samples
        }
    }

    /*
     * ============================================================
     * ÉCRITURE WAV
     * ============================================================
     */

    private fun writeWavSamples(
        wavPath: String,
        samples: ShortArray
    ) {

        val dataSize =
            samples.size.toLong() * 2L

        FileOutputStream(wavPath).use { output ->

            output.write(
                ByteArray(44)
            )

            for (sample in samples) {

                val value =
                    sample.toInt()

                output.write(
                    value and 0xFF
                )

                output.write(
                    (value shr 8) and 0xFF
                )
            }
        }

        writeWavHeader(
            wavPath,
            dataSize
        )
    }

    /*
     *
     ============================================================
     * HEADER WAV
     * ============================================================
     */

    private fun writeWavHeader(
        wavPath: String,
        dataSize: Long
    ) {

        val fileSize =
            36L + dataSize

        val header =
            ByteArray(44)

        header[0] =
            'R'.code.toByte()

        header[1] =
            'I'.code.toByte()

        header[2] =
            'F'.code.toByte()

        header[3] =
            'F'.code.toByte()

        writeIntLE(
            header,
            4,
            fileSize.toInt()
        )

        header[8] =
            'W'.code.toByte()

        header[9] =
            'A'.code.toByte()

        header[10] =
            'V'.code.toByte()

        header[11] =
            'E'.code.toByte()

        header[12] =
            'f'.code.toByte()

        header[13] =
            'm'.code.toByte()

        header[14] =
            't'.code.toByte()

        header[15] =
            ' '.code.toByte()

        writeIntLE(
            header,
            16,
            16
        )

       /*
         * PCM
         */
        writeShortLE(
            header,
            20,
            1
        )

        /*
         * Mono
         */
        writeShortLE(
            header,
            22,
            1
        )

        /*
         * 44100 Hz
         */
        writeIntLE(
            header,
            24,
            sampleRate.toInt()
        )

        /*
         * Byte rate
         */
        writeIntLE(
            header,
            28,
            sampleRate.toInt() * 2
        )

        /*
         * Block align
         */
        writeShortLE(
            header,
            32,
            2
        )

        /*
         * 16 bits
         */
        writeShortLE(
            header,
            34,
            16
        )

        header[36] =
            'd'.code.toByte()

        header[37] =
            'a'.code.toByte()

        header[38] =
            't'.code.toByte()

        header[39] =
            'a'.code.toByte()

        writeIntLE(
            header,
            40,
            dataSize.toInt()
        )

        val file =
            java.io.RandomAccessFile(
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

        buffer[offset] =
            (value and 0xFF).toByte()

        buffer[offset + 1] =
            ((value shr 8) and 0xFF)
                .toByte()

        buffer[offset + 2] =
            ((value shr 16) and 0xFF)
                .toByte()

        buffer[offset + 3] =
            ((value shr 24) and 0xFF)
                .toByte()
    }

    private fun writeShortLE(
        buffer: ByteArray,
        offset: Int,
        value: Int
    ) {

        buffer[offset] =
            (value and 0xFF).toByte()

        buffer[offset + 1] =
            ((value shr 8) and 0xFF)
                .toByte()
    }
    /*
     *
     ============================================================
     *
     COMPATIBILITÉ AVEC LE RESTE DE L'APPLICATION
     * 
     ============================================================
     */

    fun prepareVoice(
        inputWavPath: String
    ): String {

        return inputWavPath
    }

    fun isReady(): Boolean {

        return true
    }
}
