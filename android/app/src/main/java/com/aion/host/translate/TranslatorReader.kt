package com.aion.host.translate

import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.tasks.await

/** Fixed, small set rather than ML Kit's full ~60-language catalog — matches this app's own
 * real bilingual (English/Hindi) focus, plus a few common languages, over building a full
 * language-picker UI for a catalog nothing else in the app needs yet. */
enum class SupportedLanguage(
    val code: String,
    val label: String,
) {
    ENGLISH(TranslateLanguage.ENGLISH, "English"),
    HINDI(TranslateLanguage.HINDI, "Hindi"),
    SPANISH(TranslateLanguage.SPANISH, "Spanish"),
    FRENCH(TranslateLanguage.FRENCH, "French"),
    GERMAN(TranslateLanguage.GERMAN, "German"),
}

/**
 * T-161 (EPIC 17) — ML Kit Translate: fully on-device, no API key/signup, the owner's own
 * "focus mostly free" choice. The FIRST translation between a given language pair needs a
 * network connection to download that pair's model (a few dozen MB, one-time); every
 * translation after that runs fully offline.
 */
class TranslatorReader {
    suspend fun translate(
        text: String,
        from: SupportedLanguage,
        to: SupportedLanguage,
    ): String {
        val options =
            TranslatorOptions
                .Builder()
                .setSourceLanguage(from.code)
                .setTargetLanguage(to.code)
                .build()
        val translator = Translation.getClient(options)
        try {
            translator.downloadModelIfNeeded().await()
            return translator.translate(text).await()
        } finally {
            translator.close()
        }
    }
}
