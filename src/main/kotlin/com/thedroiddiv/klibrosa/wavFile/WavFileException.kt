package com.thedroiddiv.klibrosa.wavFile

/**
 * Custom Exception Class to handle errors occurring while loading/reading Wav file.
 *
 * @author abhi-rawat1
 */
class WavFileException(message: String?) : Exception(message) {
    companion object {
        private const val serialVersionUID = 1L
    }
}
