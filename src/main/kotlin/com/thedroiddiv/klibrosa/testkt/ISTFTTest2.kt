package com.thedroiddiv.klibrosa.testkt

import com.thedroiddiv.klibrosa.JLibrosa
import org.apache.commons.math3.complex.Complex
import java.io.BufferedReader
import java.io.FileReader
import java.io.IOException

object ISTFTTest2 {
    @Throws(NumberFormatException::class, IOException::class)
    @JvmStatic
    fun main(args: Array<String>) {
        // TODO Auto-generated method stub

        val compArray = readFromFile()
        val jLibrosa = JLibrosa()

        val magValues = jLibrosa.generateInvSTFTFeatures(
            compArray,
            44100, 40, 256, 128, 64
        )
        println("test")
    }

    @Throws(NumberFormatException::class, IOException::class)
    fun readFromFile(): Array<Array<Complex>> {
        val savedGameFile = "/Users/vishrud/Desktop/Vasanth/Technology/Mobile-ML/Spleeter_TF2.0/local/output2darray.csv"
        val board = Array(129) { Array(6881) { Complex(0.0) } }
        val reader = BufferedReader(FileReader(savedGameFile))
        var line = ""
        var row = 0
        while ((reader.readLine().also { line = it }) != null) {
            val cols = line.split(",".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray() //note that if you have used space as separator you have to split on " "
            var i = 0
            while (i < cols.size) {
                var procStr = cols[i].replace("[(]".toRegex(), "")
                procStr = procStr.replace("[)]".toRegex(), "")
                var splStr: Array<String>? = null

                splStr = procStr.split("xx".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val realStr = splStr[0]
                val imgStr = splStr[1]

                val real = realStr.toDouble()
                val imag = imgStr.toDouble()
                board[row][i] = Complex(real, imag)
                i = i + 1
            }


            row++
        }
        reader.close()
        return board
    }
}
