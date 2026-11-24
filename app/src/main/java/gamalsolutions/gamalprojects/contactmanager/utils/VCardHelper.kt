package gamalsolutions.gamalprojects.contactmanager.utils

import gamalsolutions.gamalprojects.contactmanager.data.model.Contact
import gamalsolutions.gamalprojects.contactmanager.data.model.ContactCategory
import gamalsolutions.gamalprojects.contactmanager.data.model.ExportOptions
import java.io.ByteArrayOutputStream

/**
 * أداة إنشاء وتحليل ملفات vCard (.vcf) وفق معيار vCard 3.0 مع دعم العربية والترميز Quoted-Printable
 * وخيارات التخصيص (البادئة، إخفاء/تضمين البريد والشركة، وتضمين التصنيف CATEGORIES).
 */
object VCardHelper {

    fun normalizePhone(rawPhone: String): String {
        val trimmed = rawPhone.trim()
        if (trimmed.isEmpty()) return ""
        val hasPlus = trimmed.startsWith("+")
        val digitsOnly = trimmed.filter { it.isDigit() }
        if (digitsOnly.isEmpty()) return ""
        return when {
            digitsOnly.startsWith("00") && digitsOnly.length > 4 -> "+${digitsOnly.drop(2)}"
            hasPlus -> "+$digitsOnly"
            else -> digitsOnly
        }
    }

    fun toVCardString(
        contacts: List<Contact>,
        options: ExportOptions = ExportOptions()
    ): String {
        if (contacts.isEmpty()) return ""
        return buildString {
            contacts.forEachIndexed { index, contact ->
                append(singleContactToVCard(contact, options))
                if (index < contacts.lastIndex) {
                    append("\r\n")
                }
            }
        }
    }

    fun singleContactToVCard(
        contact: Contact,
        options: ExportOptions = ExportOptions()
    ): String {
        val prefix = options.namePrefix.trim()
        val displayName = if (prefix.isNotEmpty() && !contact.name.startsWith(prefix)) {
            "$prefix ${contact.name.trim()}"
        } else {
            contact.name.trim()
        }

        val safeName = escapeVCardValue(displayName)
        val nameParts = displayName.split(Regex("\\s+")).filter { it.isNotEmpty() }
        val firstName = escapeVCardValue(nameParts.firstOrNull().orEmpty())
        val lastName = if (nameParts.size > 1) {
            escapeVCardValue(nameParts.drop(1).joinToString(" "))
        } else {
            ""
        }

        return buildString {
            append("BEGIN:VCARD\r\n")
            append("VERSION:3.0\r\n")
            append("FN;CHARSET=UTF-8:").append(safeName).append("\r\n")
            append("N;CHARSET=UTF-8:").append(lastName).append(";").append(firstName).append(";;;\r\n")
            if (contact.phone.isNotBlank()) {
                append("TEL;TYPE=CELL:").append(contact.phone.trim()).append("\r\n")
            }
            if (options.includeEmail && contact.email.isNotBlank()) {
                append("EMAIL;TYPE=INTERNET:").append(escapeVCardValue(contact.email.trim())).append("\r\n")
            }
            if (options.includeCompanyAndNotes) {
                if (contact.company.isNotBlank()) {
                    append("ORG;CHARSET=UTF-8:").append(escapeVCardValue(contact.company.trim())).append("\r\n")
                }
                if (contact.notes.isNotBlank()) {
                    append("NOTE;CHARSET=UTF-8:").append(escapeVCardValue(contact.notes.trim())).append("\r\n")
                }
            }
            if (contact.category != ContactCategory.GENERAL) {
                append("CATEGORIES;CHARSET=UTF-8:").append(contact.category.storageKey).append("\r\n")
            }
            append("END:VCARD")
        }
    }

    fun parseVCardString(rawContent: String): List<Contact> {
        if (rawContent.isBlank()) return emptyList()

        val unfoldedLines = unfoldLines(rawContent)
        val contacts = mutableListOf<Contact>()

        var inCard = false
        var fn = ""
        var structuredName = ""
        var phone = ""
        var email = ""
        var company = ""
        var notes = ""
        var category = ContactCategory.GENERAL

        for (rawLine in unfoldedLines) {
            val line = rawLine.trim()
            if (line.equals("BEGIN:VCARD", ignoreCase = true)) {
                inCard = true
                fn = ""
                structuredName = ""
                phone = ""
                email = ""
                company = ""
                notes = ""
                category = ContactCategory.GENERAL
                continue
            }

            if (line.equals("END:VCARD", ignoreCase = true)) {
                if (inCard) {
                    val resolvedName = fn.ifBlank { structuredName }.ifBlank { phone }
                    if (resolvedName.isNotBlank() && phone.isNotBlank()) {
                        contacts.add(
                            Contact(
                                name = resolvedName.trim(),
                                phone = phone.trim(),
                                email = email.trim(),
                                company = company.trim(),
                                notes = notes.trim(),
                                category = category
                            )
                        )
                    }
                }
                inCard = false
                continue
            }

            if (!inCard || !line.contains(":")) continue

            val colonIndex = line.indexOf(':')
            val header = line.substring(0, colonIndex)
            val rawValue = line.substring(colonIndex + 1)

            val headerParts = header.split(';')
            val propertyName = headerParts.firstOrNull()?.uppercase().orEmpty()
            val isQuotedPrintable = headerParts.any {
                it.equals("ENCODING=QUOTED-PRINTABLE", ignoreCase = true) ||
                    it.equals("QUOTED-PRINTABLE", ignoreCase = true)
            }

            val decodedValue = if (isQuotedPrintable) {
                decodeQuotedPrintable(rawValue)
            } else {
                unescapeVCardValue(rawValue)
            }

            when (propertyName) {
                "FN" -> if (fn.isBlank()) fn = decodedValue
                "N" -> {
                    if (structuredName.isBlank()) {
                        val parts = decodedValue.split(';').map { it.trim() }.filter { it.isNotEmpty() }
                        structuredName = if (parts.size >= 2) {
                            "${parts[1]} ${parts[0]}".trim()
                        } else {
                            parts.joinToString(" ")
                        }
                    }
                }
                "TEL" -> {
                    if (phone.isBlank() && decodedValue.isNotBlank()) {
                        phone = decodedValue.removePrefix("tel:").trim()
                    }
                }
                "EMAIL" -> if (email.isBlank()) email = decodedValue
                "ORG" -> if (company.isBlank()) company = decodedValue.replace(';', ' ').trim()
                "NOTE" -> if (notes.isBlank()) notes = decodedValue
                "CATEGORIES" -> {
                    val firstCat = decodedValue.split(',').firstOrNull()?.trim()
                    category = ContactCategory.fromKey(firstCat)
                }
            }
        }

        return contacts
    }

    private fun unfoldLines(rawContent: String): List<String> {
        val normalized = rawContent.replace("\r\n", "\n").replace("\r", "\n")
        val rawLines = normalized.split("\n")
        val result = mutableListOf<String>()

        for (line in rawLines) {
            if (line.startsWith(" ") || line.startsWith("\t")) {
                if (result.isNotEmpty()) {
                    result[result.lastIndex] = result.last() + line.drop(1)
                }
            } else if (result.isNotEmpty() && result.last().endsWith("=") &&
                result.last().contains("QUOTED-PRINTABLE", ignoreCase = true)
            ) {
                result[result.lastIndex] = result.last().dropLast(1) + line
            } else {
                result.add(line)
            }
        }
        return result
    }

    private fun escapeVCardValue(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace("\n", "\\n")
            .replace(";", "\\;")
            .replace(",", "\\,")
    }

    private fun unescapeVCardValue(value: String): String {
        return value
            .replace("\\n", "\n")
            .replace("\\N", "\n")
            .replace("\\;", ";")
            .replace("\\,", ",")
            .replace("\\\\", "\\")
    }

    private fun decodeQuotedPrintable(input: String): String {
        val bytes = ByteArrayOutputStream()
        var i = 0
        while (i < input.length) {
            val ch = input[i]
            if (ch == '=' && i + 2 < input.length) {
                val hex = input.substring(i + 1, i + 3)
                val byteVal = hex.toIntOrNull(16)
                if (byteVal != null) {
                    bytes.write(byteVal)
                    i += 3
                    continue
                }
            }
            bytes.write(ch.code)
            i++
        }
        return bytes.toString(Charsets.UTF_8.name())
    }
}
