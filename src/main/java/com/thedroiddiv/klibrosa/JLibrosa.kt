package com.thedroiddiv.klibrosa

import com.thedroiddiv.klibrosa.exception.FileFormatNotSupportedException
import com.thedroiddiv.klibrosa.process.AudioFeatureExtraction
import com.thedroiddiv.klibrosa.wavFile.WavFile
import com.thedroiddiv.klibrosa.wavFile.WavFileException
import org.apache.commons.math3.complex.Complex
import java.io.File
import java.io.IOException
import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.*
import java.util.stream.DoubleStream
import java.util.stream.IntStream

/**
 *
 * This Class is an equivalent of Python Librosa utility used to extract the Audio features from given Wav file.
 *
 * @author abhi-rawat1
 */
class JLibrosa {
    private val BUFFER_SIZE = 4096
    @JvmField
	var noOfFrames: Int = -1
    var sampleRate: Int = -1
        set(sampleRate) {
            field = sampleRate
            this.fMax = sampleRate / 2.0
        }
    @JvmField
	var noOfChannels: Int = -1

    private var fMax = 44100 / 2.0
    private val fMin = 0.0
    val n_fft: Int = 2048
    val hop_length: Int = 512
    val n_mels: Int = 128


    fun getfMax(): Double {
        return fMax
    }


    fun getfMin(): Double {
        return fMin
    }


    /**
     * This function is used to load the audio file and read its Numeric Magnitude
     * Feature Values.
     *
     * @param path
     * @param sr
     * @param readDurationInSec
     * @return
     * @throws IOException
     * @throws WavFileException
     * @throws FileFormatNotSupportedException
     */
    @Throws(IOException::class, WavFileException::class, FileFormatNotSupportedException::class)
    fun loadAndReadAcrossChannelsWithOffset(
        path: String,
        sr: Int,
        readDurationInSec: Int,
        offsetDuration: Int
    ): Array<FloatArray> {
        val magValues = readMagnitudeValuesFromFile(path, sr, readDurationInSec, offsetDuration)
        return magValues
    }


    /**
     * This function is used to load the audio file and read its Numeric Magnitude
     * Feature Values.
     *
     * @param path
     * @param sr
     * @param readDurationInSec
     * @return
     * @throws IOException
     * @throws WavFileException
     * @throws FileFormatNotSupportedException
     */
    @Throws(IOException::class, WavFileException::class, FileFormatNotSupportedException::class)
    fun loadAndReadAcrossChannels(path: String, sr: Int, readDurationInSec: Int): Array<FloatArray> {
        val magValues = loadAndReadAcrossChannelsWithOffset(path, sr, readDurationInSec, 0)
        return magValues
    }

    /**
     * This function is used to load the audio file and read its Numeric Magnitude
     * Feature Values.
     *
     * @param path
     * @param sr
     * @param readDurationInSeconds
     * @return
     * @throws IOException
     * @throws WavFileException
     * @throws FileFormatNotSupportedException
     */
    @Throws(IOException::class, WavFileException::class, FileFormatNotSupportedException::class)
    private fun readMagnitudeValuesFromFile(
        path: String,
        sampleRate: Int,
        readDurationInSeconds: Int,
        offsetDuration: Int
    ): Array<FloatArray> {
        if (!path.endsWith(".wav")) {
            throw FileFormatNotSupportedException("File format not supported. jLibrosa currently supports audio processing of only .wav files")
        }

        val sourceFile = File(path)
        var wavFile: WavFile? = null

        wavFile = WavFile.openWavFile(sourceFile)
        var mNumFrames = wavFile.numFrames.toInt()
        var mSampleRate = wavFile.sampleRate.toInt()
        val mChannels = wavFile.numChannels

        val totalNoOfFrames = mNumFrames
        val frameOffset = offsetDuration * mSampleRate
        var tobeReadFrames = readDurationInSeconds * mSampleRate

        if (tobeReadFrames > (totalNoOfFrames - frameOffset)) {
            tobeReadFrames = totalNoOfFrames - frameOffset
        }

        if (readDurationInSeconds != -1) {
            mNumFrames = tobeReadFrames
            wavFile.numFrames = mNumFrames.toLong()
        }


        this.noOfChannels = mChannels
        this.noOfFrames = mNumFrames
        this.sampleRate = mSampleRate


        if (sampleRate != -1) {
            mSampleRate = sampleRate
        }

        // Read the magnitude values across both the channels and save them as part of
        // multi-dimensional array
        val buffer = Array(mChannels) { FloatArray(mNumFrames) }
        var readFrameCount: Long = 0
        //for (int i = 0; i < loopCounter; i++) {
        readFrameCount = wavFile.readFrames(buffer, mNumFrames, frameOffset)

        //}
        wavFile?.close()

        return buffer
    }


    /**
     * This function calculates and returns the MFCC values of given Audio Sample
     * values.
     *
     * @param magValues
     * @param nMFCC
     * @return
     */
    fun generateMFCCFeatures(
        magValues: FloatArray?,
        mSampleRate: Int,
        nMFCC: Int,
        n_fft: Int,
        n_mels: Int,
        hop_length: Int
    ): Array<FloatArray> {
        var mSampleRate = mSampleRate
        val mfccConvert = AudioFeatureExtraction()

        mfccConvert.n_mfcc = nMFCC
        mfccConvert.n_mels = n_mels
        mfccConvert.hop_length = hop_length

        if (mSampleRate == -1) {
            mSampleRate = this.sampleRate
        }

        mfccConvert.sampleRate = mSampleRate.toDouble()
        mfccConvert.n_mfcc = nMFCC
        val mfccInput = mfccConvert.extractMFCCFeatures(magValues) //extractMFCCFeatures(magValues);

        val nFFT = mfccInput.size / nMFCC
        val mfccValues = Array(nMFCC) { FloatArray(nFFT) }

        // loop to convert the mfcc values into multi-dimensional array
        for (i in 0 until nFFT) {
            var indexCounter = i * nMFCC
            val rowIndexValue = i % nFFT
            for (j in 0 until nMFCC) {
                mfccValues[j][rowIndexValue] = mfccInput[indexCounter]
                indexCounter++
            }
        }

        return mfccValues
    }


    /**
     * This function calculates and returns the MFCC values of given Audio Sample
     * values.
     *
     * @param magValues
     * @param nMFCC
     * @return
     */
    fun generateMFCCFeatures(magValues: FloatArray?, mSampleRate: Int, nMFCC: Int): Array<FloatArray> {
        val mfccValues =
            this.generateMFCCFeatures(magValues, mSampleRate, nMFCC, this.n_fft, this.n_mels, this.hop_length)

        return mfccValues
    }

    /**
     * This function calculates and return the Mean MFCC values.
     *
     * @param mfccValues
     * @param nMFCC
     * @param nFFT
     * @return
     */
    fun generateMeanMFCCFeatures(mfccValues: Array<FloatArray>, nMFCC: Int, nFFT: Int): FloatArray {
        // code to take the mean of mfcc values across the rows such that
        // [nMFCC x nFFT] matrix would be converted into
        // [nMFCC x 1] dimension - which would act as an input to tflite model


        val meanMFCCValues = FloatArray(nMFCC)
        for (i in mfccValues.indices) {
            val floatArrValues = mfccValues[i]
            val ds = IntStream.range(0, floatArrValues.size)
                .mapToDouble { k: Int -> floatArrValues[k].toDouble() }

            val avg = DoubleStream.of(*ds.toArray()).average().asDouble
            val floatVal = avg.toFloat()
            meanMFCCValues[i] = floatVal
        }


        /*for (int p = 0; p < nMFCC; p++) {
			double fftValAcrossRow = 0;
			for (int q = 0; q < nFFT; q++) {
				fftValAcrossRow = fftValAcrossRow + mfccValues[p][q];
			}
			double fftMeanValAcrossRow = fftValAcrossRow / nFFT;
			meanMFCCValues[p] = (float) fftMeanValAcrossRow;
		} */
        return meanMFCCValues
    }

    /**
     * This function calculates and returns the melspectrogram of given Audio Sample
     * values.
     *
     * @param yValues - audio magnitude values
     * @return
     */
    fun generateMelSpectroGram(yValues: FloatArray?): Array<DoubleArray> {
        val mfccConvert = AudioFeatureExtraction()
        val melSpectrogram = mfccConvert.melSpectrogram(yValues)
        return melSpectrogram
    }


    /**
     * This function calculates and returns the me of given Audio Sample
     * values. STFT stands for Short Term Fourier Transform
     *
     * @param magValues
     * @param sampleRate
     * @param nMFCC
     * @param nFFT
     * @param nmels
     * @param hop_length
     * @return
     */
    fun generateMelSpectroGram(
        yValues: FloatArray?,
        mSampleRate: Int,
        n_fft: Int,
        n_mels: Int,
        hop_length: Int
    ): Array<FloatArray> {
        val mfccConvert = AudioFeatureExtraction()
        mfccConvert.sampleRate = mSampleRate.toDouble()
        mfccConvert.n_fft = n_fft
        mfccConvert.n_mels = n_mels
        mfccConvert.hop_length = hop_length
        val melSVal = mfccConvert.melSpectrogramWithComplexValueProcessing(yValues)
        return melSVal
    }


    /**
     * This function calculates and returns the STFT values of given Audio Sample
     * values. STFT stands for Short Term Fourier Transform
     *
     * @param magValues
     * @param nMFCC
     * @return
     */
    fun generateSTFTFeatures(
        magValues: FloatArray?,
        mSampleRate: Int,
        nMFCC: Int,
        n_fft: Int,
        n_mels: Int,
        hop_length: Int
    ): Array<Array<Complex>> {
        val stftValues =
            this.generateSTFTFeaturesWithPadOption(magValues, mSampleRate, nMFCC, n_fft, n_mels, hop_length, true)
        return stftValues
    }


    /**
     * This function calculates and returns the STFT values of given Audio Sample
     * values with/without applying padding as one of the argument flag. STFT stands for Short Term Fourier Transform
     *
     * @param magValues
     * @param nMFCC
     * @return
     */
    fun generateSTFTFeaturesWithPadOption(
        magValues: FloatArray?,
        mSampleRate: Int,
        nMFCC: Int,
        n_fft: Int,
        n_mels: Int,
        hop_length: Int,
        paddingFlag: Boolean
    ): Array<Array<Complex>> {
        var mSampleRate = mSampleRate
        val featureExtractor = AudioFeatureExtraction()
        featureExtractor.n_fft = n_fft
        featureExtractor.n_mels = n_mels
        featureExtractor.hop_length = hop_length

        if (mSampleRate == -1) {
            mSampleRate = this.sampleRate
        }

        featureExtractor.sampleRate = mSampleRate.toDouble()
        featureExtractor.n_mfcc = nMFCC
        val stftValues = featureExtractor.extractSTFTFeaturesAsComplexValues(magValues, paddingFlag)
        return stftValues
    }


    /**
     * This function calculates and returns the inverse STFT values of given stft values
     * values. STFT stands for Short Term Fourier Transform
     *
     * @param magValues
     * @param nMFCC
     * @return
     */
    fun generateInvSTFTFeatures(
        stftValues: Array<Array<Complex>>,
        mSampleRate: Int,
        nMFCC: Int,
        n_fft: Int,
        n_mels: Int,
        hop_length: Int
    ): FloatArray {
        val magValues = this.generateInvSTFTFeaturesWithPadOption(
            stftValues,
            mSampleRate,
            nMFCC,
            n_fft,
            n_mels,
            hop_length,
            -1,
            false
        )
        return magValues
    }


    /**
     * This function calculates and returns the inverse STFT values of given stft values
     * values. STFT stands for Short Term Fourier Transform
     *
     * @param magValues
     * @param nMFCC
     * @return
     */
    fun generateInvSTFTFeatures(
        stftValues: Array<Array<Complex>>,
        mSampleRate: Int,
        nMFCC: Int,
        n_fft: Int,
        n_mels: Int,
        hop_length: Int,
        length: Int
    ): FloatArray {
        val magValues = this.generateInvSTFTFeaturesWithPadOption(
            stftValues,
            mSampleRate,
            nMFCC,
            n_fft,
            n_mels,
            hop_length,
            length,
            false
        )
        return magValues
    }

    /**
     * This function calculates and returns the inverse STFT values of given stft values
     * values. STFT stands for Short Term Fourier Transform
     * This function to be used for getting inverse STFT if STFT values have been generated with pad values.
     *
     * @param magValues
     * @param nMFCC
     * @return
     */
    fun generateInvSTFTFeaturesWithPadOption(
        stftValues: Array<Array<Complex>>,
        mSampleRate: Int,
        nMFCC: Int,
        n_fft: Int,
        n_mels: Int,
        hop_length: Int,
        length: Int,
        paddingFlag: Boolean
    ): FloatArray {
        var mSampleRate = mSampleRate
        val featureExtractor = AudioFeatureExtraction()
        featureExtractor.n_fft = n_fft
        featureExtractor.n_mels = n_mels
        featureExtractor.hop_length = hop_length
        featureExtractor.length = length

        if (mSampleRate == -1) {
            mSampleRate = this.sampleRate
        }

        featureExtractor.sampleRate = mSampleRate.toDouble()
        featureExtractor.n_mfcc = nMFCC
        val magValues = featureExtractor.extractInvSTFTFeaturesAsFloatValues(stftValues, paddingFlag)
        return magValues
    }


    /**
     * This function calculates and returns the inverse STFT values of given STFT Complex
     * values. STFT stands for Short Term Fourier Transform
     *
     * @param magValues
     * @param nMFCC
     * @return
     */
    fun generateInvSTFTFeatures(stftValues: Array<Array<Complex>>, mSampleRate: Int, nMFCC: Int): FloatArray {
        val magValues =
            this.generateInvSTFTFeatures(stftValues, mSampleRate, nMFCC, this.n_fft, this.n_mels, this.hop_length)
        return magValues
    }


    /**
     * This function calculates and returns the STFT values of given Audio Sample
     * values. STFT stands for Short Term Fourier Transform
     *
     * @param magValues
     * @param nMFCC
     * @return
     */
    fun generateSTFTFeatures(magValues: FloatArray?, mSampleRate: Int, nMFCC: Int): Array<Array<Complex>> {
        val stftValues =
            this.generateSTFTFeatures(magValues, mSampleRate, nMFCC, this.n_fft, this.n_mels, this.hop_length)
        return stftValues
    }


    /**
     * This function calculates and returns the STFT values of given Audio Sample
     * values. STFT stands for Short Term Fourier Transform
     *
     * @param magValues
     * @param nMFCC
     * @return
     */
    fun generateSTFTFeaturesWithPadOption(
        magValues: FloatArray?,
        mSampleRate: Int,
        nMFCC: Int,
        padFlag: Boolean
    ): Array<Array<Complex>> {
        val stftValues = this.generateSTFTFeaturesWithPadOption(
            magValues,
            mSampleRate,
            nMFCC,
            this.n_fft,
            this.n_mels,
            this.hop_length,
            padFlag
        )
        return stftValues
    }

    /**
     * This function loads the audio file, reads its Numeric Magnitude Feature
     * values and then takes the mean of amplitude values across all the channels and
     * convert the signal to mono mode by taking the average. This method reads the audio file
     * post the mentioned offset duration in seconds.
     *
     * @param path
     * @param sampleRate
     * @param readDurationInSeconds
     * @param offsetDuration
     * @return
     * @throws IOException
     * @throws WavFileException
     * @throws FileFormatNotSupportedException
     */
    @Throws(IOException::class, WavFileException::class, FileFormatNotSupportedException::class)
    fun loadAndReadWithOffset(
        path: String,
        sampleRate: Int,
        readDurationInSeconds: Int,
        offsetDuration: Int
    ): FloatArray {
        val magValueArray = readMagnitudeValuesFromFile(path, sampleRate, readDurationInSeconds, offsetDuration)

        val df = DecimalFormat("#.#####")
        df.roundingMode = RoundingMode.CEILING

        val mNumFrames = this.noOfFrames
        val mChannels = this.noOfChannels


        // take the mean of amplitude values across all the channels and convert the
        // signal to mono mode
        val meanBuffer = FloatArray(mNumFrames)


        for (q in 0 until mNumFrames) {
            var frameVal = 0.0
            for (p in 0 until mChannels) {
                frameVal = frameVal + magValueArray[p][q]
            }
            meanBuffer[q] = df.format(frameVal / mChannels).toFloat()
        }

        return meanBuffer
    }


    /**
     * This function loads the audio file, reads its Numeric Magnitude Feature
     * values and then takes the mean of amplitude values across all the channels and
     * convert the signal to mono mode
     *
     * @param path
     * @param sampleRate
     * @param readDurationInSeconds
     * @return
     * @throws IOException
     * @throws WavFileException
     * @throws FileFormatNotSupportedException
     */
    @Throws(IOException::class, WavFileException::class, FileFormatNotSupportedException::class)
    fun loadAndRead(path: String, sampleRate: Int, readDurationInSeconds: Int): FloatArray {
        val meanBuffer = loadAndReadWithOffset(path, sampleRate, readDurationInSeconds, 0)
        return meanBuffer
    }


    /**
     * This function loads the audio file, reads its Numeric Magnitude Feature
     * values and then takes the mean of amplitude values across all the channels and
     * convert the signal to mono mode
     *
     * @param path
     * @param sampleRate
     * @param readDurationInSeconds
     * @return
     * @throws IOException
     * @throws WavFileException
     * @throws FileFormatNotSupportedException
     */
    @Throws(IOException::class, WavFileException::class, FileFormatNotSupportedException::class)
    fun loadAndReadStereoWithOffset(
        path: String,
        sampleRate: Int,
        readDurationInSeconds: Int,
        offsetDuration: Int
    ): Array<FloatArray> {
        val magValueArray = readMagnitudeValuesFromFile(path, sampleRate, readDurationInSeconds, offsetDuration)
        val mNumFrames = this.noOfFrames

        val stereoAudioArray = Array(2) { FloatArray(mNumFrames) }

        for (i in magValueArray.indices) {
            stereoAudioArray[i] = Arrays.copyOfRange(magValueArray[i], 0, mNumFrames)
        }
        return stereoAudioArray
    }


    /**
     * This function loads the audio file, reads its Numeric Magnitude Feature
     * values and then takes the mean of amplitude values across all the channels and
     * convert the signal to mono mode
     *
     * @param path
     * @param sampleRate
     * @param readDurationInSeconds
     * @return
     * @throws IOException
     * @throws WavFileException
     * @throws FileFormatNotSupportedException
     */
    @Throws(IOException::class, WavFileException::class, FileFormatNotSupportedException::class)
    fun loadAndReadStereo(path: String, sampleRate: Int, readDurationInSeconds: Int): Array<FloatArray> {
        val stereoAudioArray = loadAndReadStereoWithOffset(path, sampleRate, readDurationInSeconds, 0)
        return stereoAudioArray
    }


    /**
     * This function loads the audio file, reads its Numeric Magnitude Feature
     * values and then takes the mean of amplitude values across all the channels and
     * convert the signal to mono mode
     *
     * @param path
     * @param sampleRate
     * @param readDurationInSeconds
     * @return
     * @throws IOException
     * @throws WavFileException
     * @throws FileFormatNotSupportedException
     */
    @Throws(IOException::class, WavFileException::class, FileFormatNotSupportedException::class)
    fun loadAndReadAsListWithOffset(
        path: String,
        sampleRate: Int,
        readDurationInSeconds: Int,
        offsetDuration: Int
    ): ArrayList<Float> {
        val magValueArray = readMagnitudeValuesFromFile(path, sampleRate, readDurationInSeconds, offsetDuration)

        val df = DecimalFormat("#.#####")
        df.roundingMode = RoundingMode.CEILING

        val mNumFrames = this.noOfFrames
        val mChannels = this.noOfChannels


        // take the mean of amplitude values across all the channels and convert the
        // signal to mono mode
        val meanBuffer = FloatArray(mNumFrames)
        val meanBufferList = ArrayList<Float>()
        for (q in 0 until mNumFrames) {
            var frameVal = 0.0
            for (p in 0 until mChannels) {
                frameVal = frameVal + magValueArray[p][q]
            }
            meanBufferList.add(df.format(frameVal / mChannels).toFloat())
        }

        return meanBufferList
    }


    /**
     * This function loads the audio file, reads its Numeric Magnitude Feature
     * values and then takes the mean of amplitude values across all the channels and
     * convert the signal to mono mode
     *
     * @param path
     * @param sampleRate
     * @param readDurationInSeconds
     * @return
     * @throws IOException
     * @throws WavFileException
     * @throws FileFormatNotSupportedException
     */
    @Throws(IOException::class, WavFileException::class, FileFormatNotSupportedException::class)
    fun loadAndReadAsList(path: String, sampleRate: Int, readDurationInSeconds: Int): ArrayList<Float> {
        val meanBufferList = loadAndReadAsListWithOffset(path, sampleRate, readDurationInSeconds, 0)
        return meanBufferList
    }
}
