package com.bimantara.feature.notes.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

class NoteSpeechManager {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening: Boolean = false

    fun isAvailable(context: Context): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    fun startListening(
        context: Context,
        language: String = "id-ID",
        onReady: () -> Unit = {},
        onStatusChanged: (String) -> Unit = {},
        onRmsChanged: (Float) -> Unit = {},
        onPartialResult: (String) -> Unit = {},
        onFinalResult: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        mainHandler.post {
            if (isListening) {
                stopListening()
            }

            try {
                speechRecognizer?.destroy()
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context.applicationContext)

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, language)
                    putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, language)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                }

                speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        isListening = true
                        onReady()
                        onStatusChanged("Mendengarkan... Silakan bicara")
                    }

                    override fun onBeginningOfSpeech() {
                        onStatusChanged("Mendeteksi suara ucapan...")
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        // Normalize typical rms dB (-2 to 10 dB) to 0.0 .. 1.0 for visualizer
                        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                        onRmsChanged(normalized)
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        onStatusChanged("Memproses ucapan...")
                    }

                    override fun onError(error: Int) {
                        isListening = false
                        val errorDesc = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Masalah mikrofon atau audio"
                            SpeechRecognizer.ERROR_CLIENT -> "Selesai mendengarkan"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Izin mikrofon diperlukan"
                            SpeechRecognizer.ERROR_NETWORK -> "Koneksi jaringan bermasalah"
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Waktu koneksi jaringan habis"
                            SpeechRecognizer.ERROR_NO_MATCH -> "Tidak ada ucapan yang terdeteksi"
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Layanan pengenal suara sedang sibuk"
                            SpeechRecognizer.ERROR_SERVER -> "Kesalahan server pengenal suara"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Waktu bicara selesai"
                            else -> "Status audio ($error)"
                        }
                        Log.d("NoteSpeechManager", "SpeechRecognizer error: $error ($errorDesc)")
                        onError(errorDesc)
                    }

                    override fun onResults(results: Bundle?) {
                        isListening = false
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull()?.trim()
                        if (!text.isNullOrBlank()) {
                            onFinalResult(text)
                        } else {
                            onStatusChanged("Tidak ada teks terdeteksi")
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull()?.trim()
                        if (!text.isNullOrBlank()) {
                            onPartialResult(text)
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })

                speechRecognizer?.startListening(intent)
                isListening = true
                onStatusChanged("Menghubungkan ke mikrofon...")
            } catch (e: Exception) {
                isListening = false
                Log.e("NoteSpeechManager", "Failed to start listening", e)
                onError(e.message ?: "Gagal memulai pengenalan suara")
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isListening = false
            }
        }
    }

    fun cancel() {
        mainHandler.post {
            try {
                speechRecognizer?.cancel()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isListening = false
            }
        }
    }

    fun destroy() {
        mainHandler.post {
            try {
                speechRecognizer?.destroy()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                speechRecognizer = null
                isListening = false
            }
        }
    }
}
