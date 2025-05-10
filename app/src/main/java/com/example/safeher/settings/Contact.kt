package com.example.safeher.settings

data class Contact(
    var name: String = "",
    var phone: String = "",
    var isChecked: Boolean = false
) {
    fun toMap() = mapOf(
        "name" to name,
        "phone" to normalizePhone(phone)
    )

    fun getNormalizedPhone(): String = normalizePhone(phone)

    companion object {
        fun normalizePhone(phone: String): String {
            return phone
                .replace(Regex("[^\\d+]"), "")       // מסיר רווחים, מקפים, סוגריים וכו'
                .replace("^0".toRegex(), "+972")     // המרה מ־050 ל־+97250
        }
    }
}
