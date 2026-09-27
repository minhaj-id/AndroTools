package com.bimantara.feature.scanner.ocr

/**
 * Represents a language supported by the Tesseract OCR engine.
 */
data class OcrLanguage(
    val code: String,
    val name: String,
    val nativeName: String,
    val flagEmoji: String,
    val isBundled: Boolean = false,
    val downloadUrl: String = "https://raw.githubusercontent.com/tesseract-ocr/tessdata_fast/main/$code.traineddata",
    val estimatedSize: String = "1.5 MB"
) {
    companion object {
        val INDONESIAN = OcrLanguage(
            code = "ind",
            name = "Bahasa Indonesia",
            nativeName = "Bahasa Indonesia",
            flagEmoji = "🇮🇩",
            isBundled = true,
            estimatedSize = "1.1 MB"
        )
        val ENGLISH = OcrLanguage(
            code = "eng",
            name = "English",
            nativeName = "English",
            flagEmoji = "🇺🇸",
            isBundled = true,
            estimatedSize = "4.0 MB"
        )
        val SPANISH = OcrLanguage(
            code = "spa",
            name = "Spanyol",
            nativeName = "Español",
            flagEmoji = "🇪🇸",
            isBundled = false,
            estimatedSize = "1.3 MB"
        )
        val FRENCH = OcrLanguage(
            code = "fra",
            name = "Prancis",
            nativeName = "Français",
            flagEmoji = "🇫🇷",
            isBundled = false,
            estimatedSize = "1.4 MB"
        )
        val GERMAN = OcrLanguage(
            code = "deu",
            name = "Jerman",
            nativeName = "Deutsch",
            flagEmoji = "🇩🇪",
            isBundled = false,
            estimatedSize = "1.8 MB"
        )
        val ITALIAN = OcrLanguage(
            code = "ita",
            name = "Italia",
            nativeName = "Italiano",
            flagEmoji = "🇮🇹",
            isBundled = false,
            estimatedSize = "1.6 MB"
        )
        val ARABIC = OcrLanguage(
            code = "ara",
            name = "Arab",
            nativeName = "العربية",
            flagEmoji = "🇸🇦",
            isBundled = false,
            estimatedSize = "1.9 MB"
        )
        val JAPANESE = OcrLanguage(
            code = "jpn",
            name = "Jepang",
            nativeName = "日本語",
            flagEmoji = "🇯🇵",
            isBundled = false,
            estimatedSize = "3.2 MB"
        )
        val CHINESE = OcrLanguage(
            code = "chi_sim",
            name = "Mandarin (Aksara Sederhana)",
            nativeName = "简体中文",
            flagEmoji = "🇨🇳",
            isBundled = false,
            estimatedSize = "3.8 MB"
        )

        val ALL_LANGUAGES = listOf(
            INDONESIAN,
            ENGLISH,
            SPANISH,
            FRENCH,
            GERMAN,
            ITALIAN,
            ARABIC,
            JAPANESE,
            CHINESE
        )

        fun findByCode(code: String): OcrLanguage {
            return ALL_LANGUAGES.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: INDONESIAN
        }
    }
}
