package com.thedroiddiv.klibrosa

import com.thedroiddiv.klibrosa.exception.FileFormatNotSupportedException
import com.thedroiddiv.klibrosa.wavFile.WavFileException
import java.io.IOException
import kotlin.math.*

class FFT_iFFT {
    class Complex

    // create a new object with the given real and imaginary parts
        (// the real part
        private val re: Double, // the imaginary part
        private val im: Double
    ) {
        // return a string representation of the invoking Complex object
        override fun toString(): String {
            if (im == 0.0) return re.toString() + ""
            if (re == 0.0) return im.toString() + "i"
            if (im < 0) return re.toString() + " - " + (-im) + "i"
            return re.toString() + " + " + im + "i"
        }

        // return abs/modulus/magnitude and angle/phase/argument
        fun abs(): Double {
            return hypot(re, im)
        } // Math.sqrt(re*re + im*im)

        fun phase(): Double {
            return atan2(im, re)
        } // between -pi and pi

        // return a new Complex object whose value is (this + b)
        fun plus(b: Complex): Complex {
            val a = this // invoking object
            val real = a.re + b.re
            val imag = a.im + b.im
            return Complex(real, imag)
        }

        // return a new Complex object whose value is (this - b)
        fun minus(b: Complex): Complex {
            val a = this
            val real = a.re - b.re
            val imag = a.im - b.im
            return Complex(real, imag)
        }

        // return a new Complex object whose value is (this * b)
        fun times(b: Complex?): Complex {
            val a = this
            val real = a.re * b!!.re - a.im * b.im
            val imag = a.re * b.im + a.im * b.re
            return Complex(real, imag)
        }

        // scalar multiplication
        // return a new object whose value is (this * alpha)
        fun times(alpha: Double): Complex {
            return Complex(alpha * re, alpha * im)
        }

        // return a new Complex object whose value is the conjugate of this
        fun conjugate(): Complex {
            return Complex(re, -im)
        }

        // return a new Complex object whose value is the reciprocal of this
        fun reciprocal(): Complex {
            val scale = re * re + im * im
            return Complex(re / scale, -im / scale)
        }

        // return the real or imaginary part
        fun re(): Double {
            return re
        }

        fun im(): Double {
            return im
        }

        // return a / b
        fun divides(b: Complex): Complex {
            val a = this
            return a.times(b.reciprocal())
        }

        // return a new Complex object whose value is the complex exponential of
        // this
        fun exp(): Complex {
            return Complex(exp(re) * cos(im), exp(re) * sin(im))
        }

        // return a new Complex object whose value is the complex sine of this
        fun sin(): Complex {
            return Complex(sin(re) * cosh(im), cos(re) * sinh(im))
        }

        // return a new Complex object whose value is the complex cosine of this
        fun cos(): Complex {
            return Complex(
                cos(re) * cosh(im), -sin(re)
                        * sinh(im)
            )
        }

        // return a new Complex object whose value is the complex tangent of
        // this
        fun tan(): Complex {
            return sin().divides(cos())
        }

        companion object {
            // a static version of plus
            fun plus(a: Complex, b: Complex): Complex {
                val real = a.re + b.re
                val imag = a.im + b.im
                val sum = Complex(real, imag)
                return sum
            }

            // compute the FFT of x[], assuming its length is a power of 2
            fun fft(x: Array<Complex?>): Array<Complex?> {
                val N = x.size


                // base case
                if (N == 1) return arrayOf(x[0])


                // radix 2 Cooley-Tukey FFT
                if (N % 2 != 0) {
                    throw RuntimeException("N is not a power of 2")
                }


                // fft of even terms
                val even = arrayOfNulls<Complex>(N / 2)
                for (k in 0 until N / 2) {
                    even[k] = x[2 * k]
                }
                val q = fft(even)


                // fft of odd terms
                val odd = even // reuse the array
                for (k in 0 until N / 2) {
                    odd[k] = x[2 * k + 1]
                }
                val r = fft(odd)


                // combine
                val y = arrayOfNulls<Complex>(N)
                for (k in 0 until N / 2) {
                    val kth = -2 * k * Math.PI / N
                    val wk = Complex(cos(kth), sin(kth))
                    y[k] = q[k]!!.plus(wk.times(r[k]))
                    y[k + N / 2] = q[k]!!.minus(wk.times(r[k]))
                }
                return y
            }

            // compute the inverse FFT of x[], assuming its length is a power of 2
            fun ifft(x: Array<Complex?>): Array<Complex?> {
                val N = x.size
                var y = arrayOfNulls<Complex>(N)


                // take conjugate
                for (i in 0 until N) {
                    y[i] = x[i]!!.conjugate()
                }


                // compute forward FFT
                y = fft(y)


                // take conjugate again
                for (i in 0 until N) {
                    y[i] = y[i]!!.conjugate()
                }


                // divide by N
                for (i in 0 until N) {
                    y[i] = y[i]!!.times(1.0 / N)
                }

                return y
            }

            // display an array of Complex numbers to standard output
            fun show(x: Array<Complex?>, title: String?) {
                println(title)
                for (i in x.indices) {
                    println(x[i])
                }
                println()
            }

            @Throws(IOException::class, WavFileException::class, FileFormatNotSupportedException::class)
            @JvmStatic
            fun main(args: Array<String>) {
                val N = 8 //Integer.parseInt(args[0]);

                val audioFilePath = "audioFiles/1995-1826-0003.wav"
                val defaultSampleRate = -1 //-1 value implies the method to use default sample rate
                val defaultAudioDuration = -1 //-1 value implies the method to process complete audio duration

                val kLibrosa = KLibrosa()

                /* To read the magnitude values of audio files - equivalent to librosa.load('../audioFiles/1995-1826-0003.wav', sr=None) function */
                val audioFeatureValues = kLibrosa.loadAndRead(audioFilePath, defaultSampleRate, defaultAudioDuration)


                //Complex[][] stftComplexValues = jLibrosa.generateSTFTFeatures(audioFeatureValues, sampleRate, 40);
                val x = arrayOfNulls<Complex>(64)


                // original data
                for (i in 0..63) {
                    x[i] = Complex(audioFeatureValues[i].toDouble(), 0.0)
                }


                //show(x, "x");

                // FFT of original data
                val y = fft(x)

                //show(y, "y = fft(x)");

                // take inverse FFT
                val z = ifft(y)
                //show(z, "z = ifft(y)");
                println(1000)
            }
        }
    }
}