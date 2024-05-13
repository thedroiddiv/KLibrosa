package com.thedroiddiv.klibrosa.testkt

import com.thedroiddiv.klibrosa.JLibrosa
import com.thedroiddiv.klibrosa.exception.FileFormatNotSupportedException
import com.thedroiddiv.klibrosa.wavFile.WavFileException
import org.apache.commons.math3.complex.Complex
import java.io.IOException
import java.util.stream.IntStream

/**
 *
 * This class tests the JLibrosa functionality for extracting MFCC and STFT Audio features for given Wav file.
 *
 * @author abhi-rawat1
 */
object SpleeterTest {
    var jLibrosa: JLibrosa? = null

    @Throws(IOException::class, WavFileException::class, FileFormatNotSupportedException::class)
    @JvmStatic
    fun main(args: Array<String>) {
        val audioFilePath = "src/test/resources/AClassicEducation.wav"
        val defaultSampleRate = -1 //-1 value implies the method to use default sample rate
        val defaultAudioDuration = 20 //-1 value implies the method to process complete audio duration

        jLibrosa = JLibrosa()

        /* To read the magnitude values of audio files - equivalent to librosa.load('../audioFiles/1995-1826-0003.wav', sr=None) function */
        val stereoFeatureValues = jLibrosa!!.loadAndReadStereo(audioFilePath, defaultSampleRate, defaultAudioDuration)

        val stereoTransposeFeatValues = transposeMatrix(stereoFeatureValues)

        val array4D = Array(10) { Array(10) { Array(9) { FloatArray(8) } } }

        val stftValues = _stft(stereoTransposeFeatValues)
    }


    private fun _stft(stereoMatrix: Array<FloatArray>): Array<Array<Array<Array<Double?>>>> {
        val N = 4096
        val H = 1024
        val sampleRate = 44100

        val stftValuesList = ArrayList<Array<Array<Complex?>>>()

        for (i in stereoMatrix[0].indices) {
            val doubleStream = getColumnFromMatrix(stereoMatrix, i)

            val stftComplexValues = jLibrosa!!.generateSTFTFeatures(doubleStream, sampleRate, 40, 4096, 128, 1024)
            val transposedSTFTComplexValues = transposeMatrix(stftComplexValues)
            stftValuesList.add(transposedSTFTComplexValues)
        }

        val segmentLen = 512

        val stft3DMatrixValues = gen3DMatrixFrom2D(stftValuesList, segmentLen)


        val splitValue = (stft3DMatrixValues.size + segmentLen - 1) / segmentLen

        val stft4DMatrixValues = gen4DMatrixFrom3D(stft3DMatrixValues, splitValue, segmentLen)

        return stft4DMatrixValues
    }


    private fun gen4DMatrixFrom3D(
        stft3DMatrixValues: Array<Array<Array<Double?>>>,
        splitValue: Int,
        segmentLen: Int
    ): Array<Array<Array<Array<Double?>>>> {
        val yVal = 1024
        val zVal = stft3DMatrixValues[0][0].size

        val stft4DMatrixValues = Array(splitValue) { Array(segmentLen) { Array(yVal) { arrayOfNulls<Double>(zVal) } } }



        for (p in 0 until splitValue) {
            for (q in 0 until segmentLen) {
                val retInd = (p * segmentLen) + q
                for (r in 0 until yVal) {
                    for (s in 0 until zVal) {
                        stft4DMatrixValues[p][q][r][s] = stft3DMatrixValues[retInd][r][s]
                    }
                }
            }
        }

        return stft4DMatrixValues
    }


    private fun gen3DMatrixFrom2D(
        mat2DValuesList: ArrayList<Array<Array<Complex?>>>,
        segmentLen: Int
    ): Array<Array<Array<Double?>>> {
        val padSize = computePadSize(mat2DValuesList[0].size, segmentLen)
        val matrixXLen = mat2DValuesList[0].size + padSize

        val complex3DMatrix = Array(matrixXLen) {
            Array(
                mat2DValuesList[0][0].size
            ) { arrayOfNulls<Double>(mat2DValuesList.size) }
        }


        for (k in mat2DValuesList.indices) {
            val mat2DValues = mat2DValuesList[k]
            for (i in 0 until matrixXLen) {
                for (j in mat2DValues[0].indices) {
                    var value = 0.0
                    if (i < mat2DValues.size) {
                        value = mat2DValues[i][j]!!.abs()
                    }

                    complex3DMatrix[i][j][k] = value
                }
            }
        }
        return complex3DMatrix
    }


    private fun computePadSize(currentMatrixLen: Int, segmentLen: Int): Int {
        val tensorSize = currentMatrixLen % segmentLen
        val padSize = segmentLen - tensorSize
        return padSize
    }


    private fun getColumnFromMatrix(floatMatrix: Array<FloatArray>, column: Int): FloatArray {
        val doubleStream =
            IntStream.range(0, floatMatrix.size).mapToDouble { i: Int -> floatMatrix[i][column].toDouble() }
                .toArray()
        val floatArray = FloatArray(doubleStream.size)
        for (i in doubleStream.indices) {
            floatArray[i] = doubleStream[i].toFloat()
        }
        return floatArray
    }


    fun transposeMatrix(matrix: Array<Array<Complex?>>): Array<Array<Complex?>> {
        val m = matrix.size
        val n = matrix[0].size

        val transposedMatrix = Array(n) { arrayOfNulls<Complex>(m) }

        for (x in 0 until n) {
            for (y in 0 until m) {
                transposedMatrix[x][y] = matrix[y][x]
            }
        }

        return transposedMatrix
    }


    fun transposeMatrix(matrix: Array<FloatArray>): Array<FloatArray> {
        val m = matrix.size
        val n = matrix[0].size

        val transposedMatrix = Array(n) { FloatArray(m) }

        for (x in 0 until n) {
            for (y in 0 until m) {
                transposedMatrix[x][y] = matrix[y][x]
            }
        }

        return transposedMatrix
    }
}
