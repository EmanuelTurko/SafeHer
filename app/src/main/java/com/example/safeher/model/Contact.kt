package com.example.safeher.model

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
                .replace(Regex("[^\\d+]"), "")
                .replace("^0".toRegex(), "+972")
        }
    }
}