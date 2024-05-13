package com.thedroiddiv.klibrosa.testkt

import com.thedroiddiv.klibrosa.JLibrosa
import org.json.simple.JSONArray
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import org.json.simple.parser.ParseException
import java.io.FileNotFoundException
import java.io.FileReader
import java.io.IOException

object JLibrosaMatteoTest {
    @JvmStatic
    fun main(args: Array<String>) {
        // TODO Auto-generated method stub

        val jLibrosa = JLibrosa()

        val jsonParser = JSONParser()
        try {
            FileReader("/Users/vishrud/Downloads/example_data_experiment.json").use { reader ->
                //Read JSON file
                val obj = jsonParser.parse(reader)

                val signalObject = obj as JSONObject

                val sigArr = signalObject["raw_signal"] as JSONArray

                val sigFltArr = FloatArray(sigArr.size)

                for (i in sigArr.indices) {
                    val `val` = sigArr[i].toString().toFloat()
                    sigFltArr[i] = `val` / 32768.0f
                }


                val melSpectrogram = jLibrosa.generateMelSpectroGram(sigFltArr, 22050, 1024, 128, 128)
                println(1000)
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: ParseException) {
            e.printStackTrace()
        }
    }
}
