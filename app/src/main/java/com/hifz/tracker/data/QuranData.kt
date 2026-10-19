package com.hifz.tracker.data

/**
 * Static data for the 114 surahs, based on the standard 604-page Madani Mushaf
 * (King Fahd Complex print, Hafs 'an 'Asim).
 *
 * Positions are measured in pages from the very start of the mushaf:
 * position 0.0 is the top of page 1, position 1.0 is the top of page 2, and so on up to 604.0.
 *
 * Start pages are the standard published values. Where several surahs begin on the same
 * page, the page is shared between them in equal parts, so lengths of short surahs are
 * approximate to within a fraction of a page. The total always adds up to exactly 604 pages.
 */
data class Surah(
    val id: Int,
    val nameAr: String,
    val nameEn: String,
    val ayahs: Int,
    val startPage: Int,
    val start: Double,
    val end: Double,
) {
    val length: Double get() = end - start
    val endPage: Int get() = kotlin.math.ceil(end - 1e-6).toInt().coerceAtLeast(startPage)
    val juzStart: Int get() = Quran.juzOfPage(startPage)
    val juzEnd: Int get() = Quran.juzOfPage(endPage)
}

object Quran {
    const val TOTAL_PAGES = 604
    const val TOTAL_SURAHS = 114
    const val PAGES_PER_JUZ = 20.0

    private val START_PAGES = intArrayOf(
        1, 2, 50, 77, 106, 128, 151, 177, 187, 208,
        221, 235, 249, 255, 262, 267, 282, 293, 305, 312,
        322, 332, 342, 350, 359, 367, 377, 385, 396, 404,
        411, 415, 418, 428, 434, 440, 446, 453, 458, 467,
        477, 483, 489, 496, 499, 502, 507, 511, 515, 518,
        520, 523, 526, 528, 531, 534, 537, 542, 545, 549,
        551, 553, 554, 556, 558, 560, 562, 564, 566, 568,
        570, 572, 574, 575, 577, 578, 580, 582, 583, 585,
        586, 587, 587, 589, 590, 591, 591, 592, 593, 594,
        595, 595, 596, 596, 597, 597, 598, 598, 599, 599,
        600, 600, 601, 601, 601, 602, 602, 602, 603, 603,
        603, 604, 604, 604,
    )

    private val AYAHS = intArrayOf(
        7, 286, 200, 176, 120, 165, 206, 75, 129, 109,
        123, 111, 43, 52, 99, 128, 111, 110, 98, 135,
        112, 78, 118, 64, 77, 227, 93, 88, 69, 60,
        34, 30, 73, 54, 45, 83, 182, 88, 75, 85,
        54, 53, 89, 59, 37, 35, 38, 29, 18, 45,
        60, 49, 62, 55, 78, 96, 29, 22, 24, 13,
        14, 11, 11, 18, 12, 12, 30, 52, 52, 44,
        28, 28, 20, 56, 40, 31, 50, 40, 46, 42,
        29, 19, 36, 25, 22, 17, 19, 26, 30, 20,
        15, 21, 11, 8, 8, 19, 5, 8, 8, 11,
        11, 8, 3, 9, 5, 4, 7, 3, 6, 3,
        5, 4, 5, 6,
    )

    private val NAMES_AR = arrayOf(
        "الفاتحة", "البقرة", "آل عمران", "النساء", "المائدة", "الأنعام", "الأعراف", "الأنفال", "التوبة", "يونس",
        "هود", "يوسف", "الرعد", "إبراهيم", "الحجر", "النحل", "الإسراء", "الكهف", "مريم", "طه",
        "الأنبياء", "الحج", "المؤمنون", "النور", "الفرقان", "الشعراء", "النمل", "القصص", "العنكبوت", "الروم",
        "لقمان", "السجدة", "الأحزاب", "سبأ", "فاطر", "يس", "الصافات", "ص", "الزمر", "غافر",
        "فصلت", "الشورى", "الزخرف", "الدخان", "الجاثية", "الأحقاف", "محمد", "الفتح", "الحجرات", "ق",
        "الذاريات", "الطور", "النجم", "القمر", "الرحمن", "الواقعة", "الحديد", "المجادلة", "الحشر", "الممتحنة",
        "الصف", "الجمعة", "المنافقون", "التغابن", "الطلاق", "التحريم", "الملك", "القلم", "الحاقة", "المعارج",
        "نوح", "الجن", "المزمل", "المدثر", "القيامة", "الإنسان", "المرسلات", "النبأ", "النازعات", "عبس",
        "التكوير", "الانفطار", "المطففين", "الانشقاق", "البروج", "الطارق", "الأعلى", "الغاشية", "الفجر", "البلد",
        "الشمس", "الليل", "الضحى", "الشرح", "التين", "العلق", "القدر", "البينة", "الزلزلة", "العاديات",
        "القارعة", "التكاثر", "العصر", "الهمزة", "الفيل", "قريش", "الماعون", "الكوثر", "الكافرون", "النصر",
        "المسد", "الإخلاص", "الفلق", "الناس",
    )

    private val NAMES_EN = arrayOf(
        "Al-Fatihah", "Al-Baqarah", "Aal-Imran", "An-Nisa", "Al-Ma'idah", "Al-An'am", "Al-A'raf", "Al-Anfal", "At-Tawbah", "Yunus",
        "Hud", "Yusuf", "Ar-Ra'd", "Ibrahim", "Al-Hijr", "An-Nahl", "Al-Isra", "Al-Kahf", "Maryam", "Taha",
        "Al-Anbiya", "Al-Hajj", "Al-Mu'minun", "An-Nur", "Al-Furqan", "Ash-Shu'ara", "An-Naml", "Al-Qasas", "Al-Ankabut", "Ar-Rum",
        "Luqman", "As-Sajdah", "Al-Ahzab", "Saba", "Fatir", "Ya-Sin", "As-Saffat", "Sad", "Az-Zumar", "Ghafir",
        "Fussilat", "Ash-Shura", "Az-Zukhruf", "Ad-Dukhan", "Al-Jathiyah", "Al-Ahqaf", "Muhammad", "Al-Fath", "Al-Hujurat", "Qaf",
        "Adh-Dhariyat", "At-Tur", "An-Najm", "Al-Qamar", "Ar-Rahman", "Al-Waqi'ah", "Al-Hadid", "Al-Mujadilah", "Al-Hashr", "Al-Mumtahanah",
        "As-Saff", "Al-Jumu'ah", "Al-Munafiqun", "At-Taghabun", "At-Talaq", "At-Tahrim", "Al-Mulk", "Al-Qalam", "Al-Haqqah", "Al-Ma'arij",
        "Nuh", "Al-Jinn", "Al-Muzzammil", "Al-Muddaththir", "Al-Qiyamah", "Al-Insan", "Al-Mursalat", "An-Naba", "An-Nazi'at", "Abasa",
        "At-Takwir", "Al-Infitar", "Al-Mutaffifin", "Al-Inshiqaq", "Al-Buruj", "At-Tariq", "Al-A'la", "Al-Ghashiyah", "Al-Fajr", "Al-Balad",
        "Ash-Shams", "Al-Layl", "Ad-Duha", "Ash-Sharh", "At-Tin", "Al-Alaq", "Al-Qadr", "Al-Bayyinah", "Az-Zalzalah", "Al-Adiyat",
        "Al-Qari'ah", "At-Takathur", "Al-Asr", "Al-Humazah", "Al-Fil", "Quraysh", "Al-Ma'un", "Al-Kawthar", "Al-Kafirun", "An-Nasr",
        "Al-Masad", "Al-Ikhlas", "Al-Falaq", "An-Nas",
    )

    /** Pages where the first surah listed on that page begins at the very top of the page. */
    private val TOP_START_SHARED_PAGES = setOf(602, 603, 604)

    val surahs: List<Surah> by lazy { build() }

    fun byId(id: Int): Surah = surahs[(id - 1).coerceIn(0, TOTAL_SURAHS - 1)]

    fun juzOfPage(page: Int): Int = if (page < 22) 1 else minOf(30, (page - 2) / 20 + 1)

    private fun build(): List<Surah> {
        val starts = DoubleArray(TOTAL_SURAHS)
        var i = 0
        while (i < TOTAL_SURAHS) {
            val page = START_PAGES[i]
            var j = i
            while (j + 1 < TOTAL_SURAHS && START_PAGES[j + 1] == page) j++
            val m = j - i + 1
            for (k in 0 until m) {
                starts[i + k] = when {
                    m == 1 -> (page - 1).toDouble()
                    page in TOP_START_SHARED_PAGES -> (page - 1) + k.toDouble() / m
                    else -> (page - 1) + (k + 1).toDouble() / (m + 1)
                }
            }
            i = j + 1
        }
        return (0 until TOTAL_SURAHS).map { idx ->
            val end = if (idx == TOTAL_SURAHS - 1) TOTAL_PAGES.toDouble() else starts[idx + 1]
            Surah(
                id = idx + 1,
                nameAr = NAMES_AR[idx],
                nameEn = NAMES_EN[idx],
                ayahs = AYAHS[idx],
                startPage = START_PAGES[idx],
                start = starts[idx],
                end = end,
            )
        }
    }
}
