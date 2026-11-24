package gamalsolutions.gamalprojects.contactmanager.utils

import android.graphics.Bitmap
import android.graphics.Color
import gamalsolutions.gamalprojects.contactmanager.data.model.Contact
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

/**
 * أداة توليد رمز QR لجهة اتصال واحدة بصيغة vCard 3.0 باستخدام مكتبة ZXing.
 */
object QrCodeHelper {

    fun generateContactQrBitmap(contact: Contact, sizePx: Int = 640): Result<Bitmap> {
        return try {
            val payload = VCardHelper.singleContactToVCard(contact)
            val hints = mapOf(
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.MARGIN to 2
            )
            val bitMatrix = QRCodeWriter().encode(
                payload,
                BarcodeFormat.QR_CODE,
                sizePx,
                sizePx,
                hints
            )
            val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            for (x in 0 until sizePx) {
                for (y in 0 until sizePx) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            Result.success(bitmap)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
