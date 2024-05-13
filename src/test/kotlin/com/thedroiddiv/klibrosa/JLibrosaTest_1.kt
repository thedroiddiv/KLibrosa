package com.thedroiddiv.klibrosa

import com.thedroiddiv.klibrosa.exception.FileFormatNotSupportedException
import com.thedroiddiv.klibrosa.wavFile.WavFileException
import java.io.IOException
import java.nio.ByteBuffer


/**
 *
 * This class tests the JLibrosa functionality for extracting MFCC and STFT Audio features for given Wav file.
 *
 * @author abhi-rawat1
 */
object JLibrosaTest_1 {
    @Throws(IOException::class, WavFileException::class, FileFormatNotSupportedException::class)
    @JvmStatic
    fun main(args: Array<String>) {
        val audioFilePath = "src/test/resources/AClassicEducation.wav"
        val defaultSampleRate = -1 //-1 value implies the method to use default sample rate
        val defaultAudioDuration = 5 //-1 value implies the method to process complete audio duration

        val kLibrosa = KLibrosa()

        /* To read the magnitude values of audio files - equivalent to librosa.load('../audioFiles/1995-1826-0003.wav', sr=None) function */
        val audioFeatureValues =
            kLibrosa.loadAndReadWithOffset(audioFilePath, defaultSampleRate, defaultAudioDuration, 10)

        val audioFeatureValues1 = kLibrosa.loadAndReadWithOffset(audioFilePath, defaultSampleRate, 15, 0)

        val audioFeatureValuesList = kLibrosa.loadAndReadAsList(audioFilePath, defaultSampleRate, defaultAudioDuration)


        //writeToFile(audioFeatureValues);
        for (i in 0..9) {
            System.out.printf("%.6f%n", audioFeatureValues[i])
        }


        /* To read the no of frames present in audio file*/
        val nNoOfFrames = kLibrosa.noOfFrames


        /* To read sample rate of audio file */
        val sampleRate = kLibrosa.sampleRate


        /* To read number of channels in audio file */
        val noOfChannels = kLibrosa.noOfChannels

        val stftComplexValues = kLibrosa.generateSTFTFeaturesWithPadOption(audioFeatureValues, sampleRate, 40, true)


        val invSTFTValues = kLibrosa.generateInvSTFTFeatures(stftComplexValues, sampleRate, 40)


        val melSpectrogram = kLibrosa.generateMelSpectroGram(audioFeatureValues, sampleRate, 2048, 128, 256)

        println("/n/n")
        println("***************************************")
        println("***************************************")
        println("***************************************")
        println("/n/n")


        /* To read the MFCC values of an audio file 
		 *equivalent to librosa.feature.mfcc(x, sr, n_mfcc=40) in python
		 * */
        val mfccValues = kLibrosa.generateMFCCFeatures(audioFeatureValues, sampleRate, 40)

        val meanMFCCValues = kLibrosa.generateMeanMFCCFeatures(mfccValues, mfccValues.size, mfccValues[0].size)

        println(".......")
        println("Size of MFCC Feature Values: (" + mfccValues.size + " , " + mfccValues[0].size + " )")

        for (i in 0..0) {
            for (j in 0..9) {
                System.out.printf("%.6f%n", mfccValues[i][j])
            }
        }


        /* To read the STFT values of an audio file 
		 *equivalent to librosa.core.stft(x, sr, n_mfcc=40) in python
		 *Note STFT values return would be complex in nature with real and imaginary values.
		 * */
        val stftComplexValues1 = kLibrosa.generateSTFTFeatures(audioFeatureValues, sampleRate, 40)


        val invSTFTValues1 = kLibrosa.generateInvSTFTFeatures(stftComplexValues, sampleRate, 40)

        println(".......")
        println("Size of STFT Feature Values: (" + stftComplexValues.size + " , " + stftComplexValues[0].size + " )")



        for (i in 0..0) {
            for (j in 0..9) {
                val realValue = stftComplexValues[i][j].real
                val imagValue = stftComplexValues[i][j].imaginary
                println("Real and Imag values of STFT are $realValue,$imagValue")
            }
        }
    }


    fun encodePcmToMp3(pcm: ByteArray?) {
//      LameEncoder encoder = new LameEncoder(new javax.sound.sampled.AudioFormat(44100.0f, 16, 2, true, false), 256, MPEGMode.STEREO, Lame.QUALITY_HIGHEST, false);

        //fast  gmm2  LameEncoder encoder = new LameEncoder(new javax.sound.sampled.AudioFormat(88200.0f, 16, 2, true, false), 256, MPEGMode.STEREO, Lame.QUALITY_HIGHEST, false);


        /*
     LameEncoder encoder = new LameEncoder(new javax.sound.sampled.AudioFormat(44100.0f, 16, 2, true, false), 256, MPEGMode.STEREO, Lame.QUALITY_HIGHEST, false);


      ByteArrayOutputStream mp3 = new ByteArrayOutputStream();
      byte[] buffer = new byte[encoder.getPCMBufferSize()];

      int bytesToTransfer = Math.min(buffer.length, pcm.length);
      int bytesWritten;
      int currentPcmPosition = 0;

       while (0 < (bytesWritten = encoder.encodeBuffer(pcm, currentPcmPosition, bytesToTransfer, buffer))) {
          currentPcmPosition += bytesToTransfer;
          bytesToTransfer = Math.min(buffer.length, pcm.length - currentPcmPosition);
          //Log.e("logmessage", "current position: " + currentPcmPosition);
          mp3.write(buffer, 0, bytesWritten);
      }

      encoder.close();

      File file = new File("gtmm5.mp3");
      if (!file.exists()) {
          try {
              file.createNewFile();
          } catch (IOException e) {
              e.printStackTrace();
             // Log.e("logmessage", "cannot create file");
          }

          FileOutputStream stream = null;
          try {
              stream = new FileOutputStream("gtmm5.mp3");
              stream.write(mp3.toByteArray());
          } catch (FileNotFoundException e) {
              e.printStackTrace();
          } catch (IOException e) {
              e.printStackTrace();
          }

      }
      
      */
        //   return mp3.toByteArray();
    }


    @Throws(IOException::class)
    fun writeToFile(array: FloatArray) {
        val consByteArray = ByteArray(4 * array.size)


        for (i in array.indices) {
            val byteArray = ByteBuffer.allocate(4).putFloat(array[i] * 32767).array()
            for (k in byteArray.indices) {
                consByteArray[i * 4 + k] = byteArray[k]
            }
        }

        encodePcmToMp3(consByteArray)
    }
}
