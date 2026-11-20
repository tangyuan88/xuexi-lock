package com.zqc.studylock.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * 语音播报：基于系统 TextToSpeech。
 * 原应用另有 Azure 神经网络语音（BingTts/Mstts）实现，作为可选增强，
 * 这里给出系统 TTS 的可用基线实现，保证无网络也能朗读题目与提示。
 */
class Speech(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context, this)
    private var ready = false
    private val pending = mutableListOf<String>()

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val r = tts?.setLanguage(Locale.CHINA)
            ready = r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED
            tts?.setSpeechRate(1.0f)
            pending.toList().forEach { speak(it) }
            pending.clear()
        }
    }

    fun speak(text: String) {
        if (text.isBlank()) return
        if (!ready) { pending.add(text); return }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "studylock_${System.currentTimeMillis()}")
    }

    fun shutdown() {
        tts?.stop(); tts?.shutdown(); tts = null; ready = false
    }
}
