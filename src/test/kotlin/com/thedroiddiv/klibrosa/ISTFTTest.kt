package com.thedroiddiv.klibrosa

import org.apache.commons.math3.complex.Complex
import java.io.BufferedReader
import java.io.FileReader
import java.io.IOException

object ISTFTTest {
    @Throws(NumberFormatException::class, IOException::class)
    @JvmStatic
    fun main(args: Array<String>) {
        // TODO Auto-generated method stub

        val compArray = readFromFile()
        val jLibrosa = JLibrosa()

        val magValues = jLibrosa.generateInvSTFTFeatures(
            compArray,
            44100, 40, 4096, 128, 1024
        )
    }

    @Throws(NumberFormatException::class, IOException::class)
    fun readFromFile(): Array<Array<Complex>> {
        val savedGameFile = "/Users/vishrud/Downloads/twodarray.txt"
        val board = Array(2049) { Array(212) { Complex(0.0) } }
        val reader = BufferedReader(FileReader(savedGameFile))
        var line = ""
        var row = 0
        while ((reader.readLine().also { line = it }) != null) {
            val cols = line.split(",".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray() //note that if you have used space as separator you have to split on " "
            val col = 0
            val counter = 0
            var i = 0
            while (i < cols.size) {
                val realStr = cols[i].replace("[(]".toRegex(), "")
                val imagStr = cols[i + 1].replace("[)]".toRegex(), "")

                val real = realStr.toDouble()
                val imag = imagStr.toDouble()
                board[row][counter] = Complex(real, imag)
                i = i + 2
            }


            row++
        }
        reader.close()
        return board
    }
}
