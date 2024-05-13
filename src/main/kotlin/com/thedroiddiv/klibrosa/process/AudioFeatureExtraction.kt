package com.thedroiddiv.klibrosa.process

import org.apache.commons.math3.complex.Complex
import org.apache.commons.math3.transform.DftNormalization
import org.apache.commons.math3.transform.FastFourierTransformer
import org.apache.commons.math3.transform.TransformType
import java.lang.Math.pow
import kotlin.math.*

/**
 * This Class calculates the MFCC, STFT values of given audio samples.
 *
 * Source based on https://github.com/chiachunfu/speech/blob/master/speechandroid/src/org/tensorflow/demo/mfcc/MFCC.java
 *
 * @author abhi-rawat1
 */
class AudioFeatureExtraction {
    /**
     * Variable for holding n_mfcc value
     *
     * @param n_mfccVal
     */
    var n_mfcc: Int = 40
    var sampleRate: Double = 44100.0
        set(value) {
            this.fMax = value / 2.0
            field = value
        }


    var length: Int = -1
    private var fMax = sampleRate / 2.0
    private var fMin = 0.0
    var n_fft: Int = 2048
    var hop_length: Int = 512
    var n_mels: Int = 128

    /**
     * This function extract MFCC values from given Audio Magnitude Values.
     *
     * @param doubleInputBuffer
     * @return
     */
    fun extractMFCCFeatures(doubleInputBuffer: FloatArray): FloatArray {
        val mfccResult = dctMfcc(doubleInputBuffer)
        return finalshape(mfccResult)
    }

    /**
     * This function converts 2D MFCC values into 1d
     *
     * @param mfccSpecTro
     * @return
     */
    private fun finalshape(mfccSpecTro: Array<DoubleArray>): FloatArray {
        val finalMfcc = FloatArray(mfccSpecTro[0].size * mfccSpecTro.size)
        var k = 0
        for (i in mfccSpecTro[0].indices) {
            for (j in mfccSpecTro.indices) {
                finalMfcc[k] = mfccSpecTro[j][i].toFloat()
                k = k + 1
            }
        }
        return finalMfcc
    }

    /**
     * This function converts DCT values into mfcc
     *
     * @param y
     * @return
     */
    private fun dctMfcc(y: FloatArray): Array<DoubleArray> {
        val specTroGram = powerToDb(melSpectrogram(y))
        val dctBasis = dctFilter(n_mfcc, n_mels)
        val mfccSpecTro = Array(n_mfcc) { DoubleArray(specTroGram[0].size) }
        for (i in 0 until n_mfcc) {
            for (j in specTroGram[0].indices) {
                for (k in specTroGram.indices) {
                    mfccSpecTro[i][j] += dctBasis[i][k] * specTroGram[k][j]
                }
            }
        }
        return mfccSpecTro
    }

    /**
     * This function generates mel spectrogram values
     *
     * @param y
     * @return
     */
    fun melSpectrogram(y: FloatArray): Array<DoubleArray> {
        val melBasis = melFilter()
        val spectro = extractSTFTFeatures(y)
        val melS = Array(melBasis.size) { DoubleArray(spectro[0].size) }
        for (i in melBasis.indices) {
            for (j in spectro[0].indices) {
                for (k in melBasis[0].indices) {
                    melS[i][j] += melBasis[i][k] * spectro[k][j]
                }
            }
        }
        return melS
    }


    /**
     * This function generates mel spectrogram values with extracted STFT features as complex values
     *
     * @param y
     * @return
     */
    fun melSpectrogramWithComplexValueProcessing(y: FloatArray): Array<FloatArray> {
        val spectro = extractSTFTFeaturesAsComplexValues(y, true)
        val spectroAbsVal = Array(spectro.size) { DoubleArray(spectro[0].size) }

        for (i in spectro.indices) {
            for (j in spectro[0].indices) {
                val complexVal = spectro[i][j]
                val spectroDblVal = sqrt((complexVal!!.real.pow(2.0) + complexVal.imaginary.pow(2.0)))
                spectroAbsVal[i][j] = spectroDblVal.pow(2.0)
            }
        }

        val melBasis = melFilter()
        val melS = Array(melBasis.size) { FloatArray(spectro[0].size) }
        for (i in melBasis.indices) {
            for (j in spectro[0].indices) {
                for (k in melBasis[0].indices) {
                    melS[i][j] += (melBasis[i][k] * spectroAbsVal[k][j]).toFloat()
                }
            }
        }
        return melS
    }


    fun stftMagSpec(y: DoubleArray?): Array<DoubleArray> {
        //Short-time Fourier transform (STFT)
        var y = y
        val fftwin = window
        //pad y with reflect mode so it's centered. This reflect padding implementation is
        // not perfect but works for this demo.
        val ypad = DoubleArray(n_fft + y!!.size)
        for (i in 0 until n_fft / 2) {
            ypad[n_fft / 2 - i - 1] = y[i + 1]
            ypad[n_fft / 2 + y.size + i] = y[y.size - 2 - i]
        }
        for (j in y.indices) {
            ypad[n_fft / 2 + j] = y[j]
        }

        y = null

        val frame = yFrame(ypad)
        val fftmagSpec = Array(1 + n_fft / 2) { DoubleArray(frame[0].size) }
        val fftFrame = DoubleArray(n_fft)

        for (k in frame[0].indices) {
            var fftFrameCounter = 0

            for (l in 0 until n_fft) {
                fftFrame[l] = fftwin[l] * frame[l][k]
                fftFrameCounter = fftFrameCounter + 1
            }

            val tempConversion = DoubleArray(fftFrame.size)
            val tempImag = DoubleArray(fftFrame.size)

            val transformer = FastFourierTransformer(DftNormalization.STANDARD)
            try {
                val complx = transformer.transform(fftFrame, TransformType.FORWARD)

                for (i in complx.indices) {
                    val rr = (complx[i].real)

                    val ri = (complx[i].imaginary)

                    tempConversion[i] = rr * rr + ri * ri
                    tempImag[i] = ri
                }
            } catch (e: IllegalArgumentException) {
                println(e)
            }


            val magSpec = tempConversion
            for (i in 0 until 1 + n_fft / 2) {
                fftmagSpec[i][k] = magSpec[i]
            }
        }
        return fftmagSpec
    }


    /**
     * This function extracts the STFT values as complex values
     *
     * @param y
     * @return
     */
    fun extractSTFTFeaturesAsComplexValues(y: FloatArray, paddingFlag: Boolean): Array<Array<Complex>> {
        // Short-time Fourier transform (STFT)

        val fftwin = window


        // pad y with reflect mode so it's centered. This reflect padding implementation
        // is
        val frame = padFrame(y, paddingFlag)
        val fftmagSpec = Array(1 + n_fft / 2) {
            DoubleArray(
                frame!![0].size
            )
        }

        val fftFrame = DoubleArray(n_fft)

        val complex2DArray = Array(1 + n_fft / 2) {
            Array(frame!![0].size) { Complex(0.0) }
        }
        val cmplx1DArr = arrayOfNulls<Complex>(n_fft)


        val invFrame = Array(n_fft) { FloatArray(frame!![0].size) }

        for (k in frame!![0].indices) {
            var fftFrameCounter = 0
            for (l in 0 until n_fft) {
                fftFrame[fftFrameCounter] = fftwin[l] * frame[l][k]
                fftFrameCounter = fftFrameCounter + 1
            }

            val tempConversion = DoubleArray(fftFrame.size)
            val tempImag = DoubleArray(fftFrame.size)

            val transformer = FastFourierTransformer(DftNormalization.STANDARD)

            try {
                val complx = transformer.transform(fftFrame, TransformType.FORWARD)

                val Invcomplx = transformer.transform(complx, TransformType.INVERSE)


                //FFT transformed data will be over the length of FFT
                //data will be sinusoidal in nature - so taking the values of 1+n_fft/2 only for processing
                for (i in 0 until 1 + n_fft / 2) {
                    complex2DArray[i][k] = complx[i]
                }


                val cmplxINV1DArr = arrayOfNulls<Complex>(n_fft)


                for (j in 0 until 1 + n_fft / 2) {
                    cmplxINV1DArr[j] = complex2DArray[j][k]
                }

                var j_index = 2
                for (k1 in 1 + n_fft / 2 until n_fft) {
                    cmplxINV1DArr[k1] = Complex(
                        cmplxINV1DArr[k1 - j_index]!!.real, -1 * cmplxINV1DArr[k1 - j_index]!!.imaginary
                    )
                    j_index = j_index + 2
                }

                val complx1 = transformer.transform(cmplxINV1DArr, TransformType.INVERSE)


                for (p in complx1.indices) {
                    if (fftwin[p] != 0.0) {
                        invFrame[p][k] = (complx1[p].real / fftwin[p]).toFloat()
                        //invFrame[p][i] = (float) (complx[p].getReal() * fftwin[p]);
                    } else {
                        invFrame[p][k] = 0f
                    }
                }
            } catch (e: IllegalArgumentException) {
                println(e)
            }
        }


        val yValues = FloatArray((hop_length * (invFrame[0].size - 1) + n_fft))

        for (i in 0 until n_fft) {
            for (j in invFrame[0].indices) {
                yValues[j * hop_length + i] = invFrame[i][j]
            }
        }


        return complex2DArray
    }


    /**
     * This function extracts the inverse STFT values as complex values
     *
     * @param y
     * @return
     */
    fun extractInvSTFTFeaturesAsFloatValues(cmplxSTFTValues: Array<Array<Complex>>, paddingFlag: Boolean): FloatArray {
        val n_fft = 2 * (cmplxSTFTValues.size - 1)


        val n_frames = cmplxSTFTValues[0].size


        var length = ((n_frames - 1) * hop_length) + n_fft

        if (this.length != -1) {
            length = this.length
        }


        // Short-time Fourier transform (STFT)
        val fftwin = window

        val invFrame = Array(n_fft) { FloatArray(n_frames) }

        var complx: Array<Complex>? = null

        for (i in cmplxSTFTValues[0].indices) {
            val cmplx1DArr = arrayOfNulls<Complex>(n_fft)
            for (j in 0 until 1 + n_fft / 2) {
                cmplx1DArr[j] = cmplxSTFTValues[j][i]
            }


            //processed FFT values would be of length 1+n_fft/2
            //to peform inv FFT, we need to recreate the values back to the length of n_fft
            //as the values are inverse in nature - recreating the second half from the first off
            //for n_fft value of 4096 - value at the index of 2049 and 2047 will be same and the sequence will continue for (2050-2046), (2051-2045) etc
            //below loop will recreate those values
            var j_index = 2
            for (k in 1 + n_fft / 2 until n_fft) {
                cmplx1DArr[k] = Complex(
                    cmplx1DArr[k - j_index]!!.real, -1 * cmplx1DArr[k - j_index]!!.imaginary
                )
                j_index = j_index + 2
            }


            val transformer = FastFourierTransformer(DftNormalization.STANDARD)


            try {
                complx = transformer.transform(cmplx1DArr, TransformType.INVERSE)
            } catch (e: IllegalArgumentException) {
                println(e)
            }

            for (p in complx!!.indices) {
                if (fftwin[p] != 0.0) {
                    invFrame[p][i] = (complx[p].real / fftwin[p]).toFloat()
                    //invFrame[p][i] = (float) (complx[p].getReal() * fftwin[p]);
                } else {
                    invFrame[p][i] = 0f
                }
            }
        }


        val yValues = FloatArray(length)

        for (i in 0 until n_fft) {
            for (j in 0 until n_frames) {
                yValues[j * hop_length + i] = invFrame[i][j]
            }
        }


        val yValues_unpadded = FloatArray(yValues.size - n_fft)

        if (paddingFlag) {
            for (i in yValues_unpadded.indices) {
                yValues_unpadded[i] = yValues[n_fft / 2 + i]
            }

            return yValues_unpadded
        }


        return yValues
    }


    private fun padCenter(fftwin: DoubleArray, size: Int): Array<DoubleArray> {
        val n = fftwin.size

        val lpad = ((size - n) / 2)


        //this is a temp way for padding...this code needs to be updated as per pad_center method of istft function
        val fftWin2D = Array(n) { DoubleArray(1) }

        for (i in 0 until n) {
            fftWin2D[i][0] = fftwin[i]
        }

        return fftWin2D
    }

    /**
     * This function pads the y values
     *
     * @param y
     * @return
     */
    private fun padFrame(yValues: FloatArray, paddingFlag: Boolean): Array<DoubleArray>? {
        var frame: Array<DoubleArray>? = null

        if (paddingFlag) {
            val ypad = DoubleArray(n_fft + yValues.size)
            for (i in 0 until n_fft / 2) {
                ypad[n_fft / 2 - i - 1] = yValues[i + 1].toDouble()
                ypad[n_fft / 2 + yValues.size + i] = yValues[yValues.size - 2 - i].toDouble()
            }
            for (j in yValues.indices) {
                ypad[n_fft / 2 + j] = yValues[j].toDouble()
            }

            frame = yFrame(ypad)
        } else {
            val yDblValues = DoubleArray(yValues.size)
            for (i in yValues.indices) {
                yDblValues[i] = yValues[i].toDouble()
            }

            frame = yFrame(yDblValues)
        }

        return frame
    }

    /**
     * This function extract STFT values from given Audio Magnitude Values.
     *
     * @param y
     * @return
     */
    fun extractSTFTFeatures(y: FloatArray): Array<DoubleArray> {
        // Short-time Fourier transform (STFT)
        val fftwin = window


        // pad y with reflect mode so it's centered. This reflect padding implementation
        // is
        val frame = padFrame(y, true)
        val fftmagSpec = Array(1 + n_fft / 2) {
            DoubleArray(
                frame!![0].size
            )
        }

        val fftFrame = DoubleArray(n_fft)

        for (k in frame!![0].indices) {
            var fftFrameCounter = 0
            for (l in 0 until n_fft) {
                fftFrame[fftFrameCounter] = fftwin[l] * frame[l][k]
                fftFrameCounter = fftFrameCounter + 1
            }

            val tempConversion = DoubleArray(fftFrame.size)
            val tempImag = DoubleArray(fftFrame.size)

            val transformer = FastFourierTransformer(DftNormalization.STANDARD)



            try {
                val complx = transformer.transform(fftFrame, TransformType.FORWARD)

                for (i in complx.indices) {
                    val rr = (complx[i].real)

                    val ri = (complx[i].imaginary)

                    tempConversion[i] = rr * rr + ri * ri
                    tempImag[i] = ri
                }
            } catch (e: IllegalArgumentException) {
                println(e)
            }


            val magSpec = tempConversion
            for (i in 0 until 1 + n_fft / 2) {
                fftmagSpec[i][k] = magSpec[i]
            }
        }
        return fftmagSpec
    }

    private val window: DoubleArray
        /**
         * This function is used to get hann window, librosa
         *
         * @return
         */
        get() {
            // Return a Hann window for even n_fft.
            // The Hann window is a taper formed by using a raised cosine or sine-squared
            // with ends that touch zero.
            val win = DoubleArray(n_fft)
            for (i in 0 until n_fft) {
                win[i] = 0.5 - 0.5 * cos(2.0 * Math.PI * i / n_fft)
            }
            return win
        }

    /**
     * This function is used to apply padding and return Frame
     *
     * @param ypad
     * @return
     */
    private fun yFrame(ypad: DoubleArray): Array<DoubleArray> {
        val n_frames = 1 + (ypad.size - n_fft) / hop_length

        val winFrames = Array(n_fft) { DoubleArray(n_frames) }

        for (i in 0 until n_fft) {
            for (j in 0 until n_frames) {
                winFrames[i][j] = ypad[j * hop_length + i]
            }
        }
        return winFrames
    }

    /**
     * This function is used to convert Power Spectrogram values into db values.
     *
     * @param melS
     * @return
     */
    private fun powerToDb(melS: Array<DoubleArray>): Array<DoubleArray> {
        // Convert a power spectrogram (amplitude squared) to decibel (dB) units
        // This computes the scaling ``10 * log10(S / ref)`` in a numerically
        // stable way.
        val log_spec = Array(melS.size) { DoubleArray(melS[0].size) }
        var maxValue = -100.0
        for (i in melS.indices) {
            for (j in melS[0].indices) {
                val magnitude = abs(melS[i][j])
                if (magnitude > 1e-10) {
                    log_spec[i][j] = 10.0 * log10(magnitude)
                } else {
                    log_spec[i][j] = 10.0 * (-10)
                }
                if (log_spec[i][j] > maxValue) {
                    maxValue = log_spec[i][j]
                }
            }
        }

        // set top_db to 80.0
        for (i in melS.indices) {
            for (j in melS[0].indices) {
                if (log_spec[i][j] < maxValue - 80.0) {
                    log_spec[i][j] = maxValue - 80.0
                }
            }
        }
        // ref is disabled, maybe later.
        return log_spec
    }

    /**
     * This function is used to get dct filters.
     *
     * @param n_filters
     * @param n_input
     * @return
     */
    private fun dctFilter(n_filters: Int, n_input: Int): Array<DoubleArray> {
        // Discrete cosine transform (DCT type-III) basis.
        val basis = Array(n_filters) { DoubleArray(n_input) }
        val samples = DoubleArray(n_input)
        for (i in 0 until n_input) {
            samples[i] = (1 + 2 * i) * Math.PI / (2.0 * (n_input))
        }
        for (j in 0 until n_input) {
            basis[0][j] = 1.0 / sqrt(n_input.toDouble())
        }
        for (i in 1 until n_filters) {
            for (j in 0 until n_input) {
                basis[i][j] = cos(i * samples[j]) * sqrt(2.0 / (n_input))
            }
        }
        return basis
    }

    /**
     * This function is used to create a Filterbank matrix to combine FFT bins into
     * Mel-frequency bins.
     *
     * @return
     */
    private fun melFilter(): Array<DoubleArray> {
        // Create a Filterbank matrix to combine FFT bins into Mel-frequency bins.
        // Center freqs of each FFT bin
        val fftFreqs = fftFreq()
        // 'Center freqs' of mel bands - uniformly spaced between limits
        val melF = melFreq(n_mels + 2)

        val fdiff = DoubleArray(melF.size - 1)
        for (i in 0 until melF.size - 1) {
            fdiff[i] = melF[i + 1] - melF[i]
        }

        val ramps = Array(melF.size) { DoubleArray(fftFreqs.size) }
        for (i in melF.indices) {
            for (j in fftFreqs.indices) {
                ramps[i][j] = melF[i] - fftFreqs[j]
            }
        }

        val weights = Array(n_mels) { DoubleArray(1 + n_fft / 2) }
        for (i in 0 until n_mels) {
            for (j in fftFreqs.indices) {
                val lowerF = -ramps[i][j] / fdiff[i]
                val upperF = ramps[i + 2][j] / fdiff[i + 1]
                if (lowerF > upperF && upperF > 0) {
                    weights[i][j] = upperF
                } else if (lowerF > upperF && upperF < 0) {
                    weights[i][j] = 0.0
                } else if (lowerF < upperF && lowerF > 0) {
                    weights[i][j] = lowerF
                } else if (lowerF < upperF && lowerF < 0) {
                    weights[i][j] = 0.0
                } else {
                }
            }
        }

        val enorm = DoubleArray(n_mels)
        for (i in 0 until n_mels) {
            enorm[i] = 2.0 / (melF[i + 2] - melF[i])
            for (j in fftFreqs.indices) {
                weights[i][j] *= enorm[i]
            }
        }
        return weights

        // need to check if there's an empty channel somewhere
    }

    /**
     * To get fft frequencies
     *
     * @return
     */
    private fun fftFreq(): DoubleArray {
        // Alternative implementation of np.fft.fftfreqs
        val freqs = DoubleArray(1 + n_fft / 2)
        for (i in 0 until 1 + n_fft / 2) {
            freqs[i] = 0 + (sampleRate / 2) / (n_fft / 2) * i
        }
        return freqs
    }

    /**
     * To get mel frequencies
     *
     * @param numMels
     * @return
     */
    private fun melFreq(numMels: Int): DoubleArray {
        // 'Center freqs' of mel bands - uniformly spaced between limits
        val LowFFreq = DoubleArray(1)
        val HighFFreq = DoubleArray(1)
        LowFFreq[0] = fMin
        HighFFreq[0] = fMax
        val melFLow = freqToMel(LowFFreq)
        val melFHigh = freqToMel(HighFFreq)
        val mels = DoubleArray(numMels)
        for (i in 0 until numMels) {
            mels[i] = melFLow[0] + (melFHigh[0] - melFLow[0]) / (numMels - 1) * i
        }
        return melToFreq(mels)
    }

    /**
     * To convert mel frequencies into hz frequencies
     *
     * @param mels
     * @return
     */
    private fun melToFreqS(mels: DoubleArray): DoubleArray {
        val freqs = DoubleArray(mels.size)
        for (i in mels.indices) {
            freqs[i] = 700.0 * (pow(10.0, (mels[i] / 2595.0) - 1.0))
        }
        return freqs
    }

    /**
     * To convert hz frequencies into mel frequencies.
     *
     * @param freqs
     * @return
     */
    protected fun freqToMelS(freqs: DoubleArray): DoubleArray {
        val mels = DoubleArray(freqs.size)
        for (i in freqs.indices) {
            mels[i] = 2595.0 * log10(1.0 + freqs[i] / 700.0)
        }
        return mels
    }

    /**
     * To convert mel frequencies into hz frequencies
     *
     * @param mels
     * @return
     */
    private fun melToFreq(mels: DoubleArray): DoubleArray {
        // Fill in the linear scale
        val f_min = 0.0
        val f_sp = 200.0 / 3
        val freqs = DoubleArray(mels.size)

        // And now the nonlinear scale
        val min_log_hz = 1000.0 // beginning of log region (Hz)
        val min_log_mel = (min_log_hz - f_min) / f_sp // same (Mels)
        val logstep = ln(6.4) / 27.0

        for (i in mels.indices) {
            if (mels[i] < min_log_mel) {
                freqs[i] = f_min + f_sp * mels[i]
            } else {
                freqs[i] = min_log_hz * exp(logstep * (mels[i] - min_log_mel))
            }
        }
        return freqs
    }

    /**
     * To convert hz frequencies into mel frequencies
     *
     * @param freqs
     * @return
     */
    protected fun freqToMel(freqs: DoubleArray): DoubleArray {
        val f_min = 0.0
        val f_sp = 200.0 / 3
        val mels = DoubleArray(freqs.size)

        // Fill in the log-scale part
        val min_log_hz = 1000.0 // beginning of log region (Hz)
        val min_log_mel = (min_log_hz - f_min) / f_sp // # same (Mels)
        val logstep = ln(6.4) / 27.0 // step size for log region

        for (i in freqs.indices) {
            if (freqs[i] < min_log_hz) {
                mels[i] = (freqs[i] - f_min) / f_sp
            } else {
                mels[i] = min_log_mel + ln(freqs[i] / min_log_hz) / logstep
            }
        }
        return mels
    }

    /**
     * To get log10 value.
     *
     * @param value
     * @return
     */
    private fun log10(value: Double): Double {
        return ln(value) / ln(10.0)
    }
}
