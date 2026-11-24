package gamalsolutions.gamalprojects.contactmanager.utils

import gamalsolutions.gamalprojects.contactmanager.data.model.Contact
import org.junit.Assert.assertEquals
import org.junit.Test

class VCardHelperTest {

    @Test
    fun roundTripArabicAndEnglishContactsVCard30() {
        val contacts = listOf(
            Contact(
                name = "أحمد محمد",
                phone = "+966501234567",
                email = "ahmed@example.com",
                company = "شركة التقنية"
            ),
            Contact(
                name = "Sarah Ali",
                phone = "+971 50 987 6543",
                company = "Cloud Co"
            )
        )

        val vcf = VCardHelper.toVCardString(contacts)
        val parsed = VCardHelper.parseVCardString(vcf)

        assertEquals(2, parsed.size)
        assertEquals("أحمد محمد", parsed[0].name)
        assertEquals("+966501234567", parsed[0].phone)
        assertEquals("ahmed@example.com", parsed[0].email)
        assertEquals("شركة التقنية", parsed[0].company)
        assertEquals("Sarah Ali", parsed[1].name)
        assertEquals("+971 50 987 6543", parsed[1].phone)
    }

    @Test
    fun parseQuotedPrintableVCard21() {
        val vcf21 = """
            BEGIN:VCARD
            VERSION:2.1
            N;CHARSET=UTF-8;ENCODING=QUOTED-PRINTABLE:;=D8=AE=D8=A7=D9=84=D8=AF;;;
            FN;CHARSET=UTF-8;ENCODING=QUOTED-PRINTABLE:=D8=AE=D8=A7=D9=84=D8=AF =D8=B9=D9=84=D9=8A
            TEL;CELL:+201001234567
            END:VCARD
        """.trimIndent()

        val parsed = VCardHelper.parseVCardString(vcf21)
        assertEquals(1, parsed.size)
        assertEquals("خالد علي", parsed[0].name)
        assertEquals("+201001234567", parsed[0].phone)
    }

    @Test
    fun normalizePhoneHandlesSpacesAnd00Prefix() {
        assertEquals("+966501234567", VCardHelper.normalizePhone("00966 50 123-4567"))
        assertEquals("+966501234567", VCardHelper.normalizePhone("+966 (50) 123 4567"))
        assertEquals("0501234567", VCardHelper.normalizePhone("050 123 4567"))
    }
}
