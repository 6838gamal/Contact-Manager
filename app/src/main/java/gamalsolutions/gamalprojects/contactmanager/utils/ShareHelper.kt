package gamalsolutions.gamalprojects.contactmanager.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import gamalsolutions.gamalprojects.contactmanager.data.model.ExportTarget
import gamalsolutions.gamalprojects.contactmanager.data.model.WhatsAppType

/**
 * أداة المشاركة والتصدير إلى WhatsApp و WhatsApp Business و Telegram أو أي تطبيق آخر.
 *
 * ملاحظات تقنية مهمة حول التكامل:
 * - لا يمكن قراءة جهات اتصال WhatsApp أو Telegram مباشرةً لأن قواعد بياناتها خاصة ومحمية بصلاحيات النظام (تتطلب Root).
 * - الحل الآمن والمتوافق مع سياسات Google Play هو التصدير والاستيراد عبر معيار vCard (.vcf) ومشاركة Intent.
 * - عند إضافة جهة اتصال إلى دفتر الهاتف (ContactsContract)، تصبح مرئية تلقائياً داخل WhatsApp و Telegram.
 * - التصدير إلى WhatsApp يتم بإرسال ملف .vcf كمرفق عبر Intent.ACTION_SEND مع نوع text/x-vcard.
 */
object ShareHelper {

    fun isPackageInstalled(context: Context, packageName: String): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(packageName, 0)
            }
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * مشاركة ملف .vcf إلى الوجهة المحددة، مع الانتقال لمتجر Play تلقائياً إذا لم يكن التطبيق مثبتاً.
     */
    fun shareVCardUri(
        context: Context,
        vCardUri: Uri,
        target: ExportTarget,
        chooserTitle: String
    ): Result<Boolean> {
        return try {
            val targetPackage = target.packageName
            if (targetPackage != null && !isPackageInstalled(context, targetPackage)) {
                openPlayStore(context, targetPackage)
                return Result.success(false)
            }

            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/x-vcard"
                putExtra(Intent.EXTRA_STREAM, vCardUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                if (targetPackage != null) {
                    setPackage(targetPackage)
                }
            }

            val launchIntent = if (targetPackage == null) {
                Intent.createChooser(sendIntent, chooserTitle).apply {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } else {
                sendIntent.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }

            context.startActivity(launchIntent)
            Result.success(true)
        } catch (e: ActivityNotFoundException) {
            target.packageName?.let { openPlayStore(context, it) }
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * فتح محادثة واتساب مباشرة عبر رابط wa.me/{number} مع دعم WhatsApp Business.
     */
    fun openWhatsAppChat(
        context: Context,
        internationalDigits: String,
        whatsAppType: WhatsAppType
    ): Result<Boolean> {
        return try {
            val url = "https://wa.me/$internationalDigits"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (isPackageInstalled(context, whatsAppType.packageName)) {
                    setPackage(whatsAppType.packageName)
                }
            }
            context.startActivity(intent)
            Result.success(true)
        } catch (e: ActivityNotFoundException) {
            // إذا لم يتوفر التطبيق المستهدف، نفتح عبر المتصفح أو المتجر
            try {
                val fallback = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://wa.me/$internationalDigits")
                ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                context.startActivity(fallback)
                Result.success(true)
            } catch (ex: Exception) {
                Result.failure(ex)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun openPlayStore(context: Context, packageName: String) {
        try {
            val marketIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("market://details?id=$packageName")
            ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            context.startActivity(marketIntent)
        } catch (_: Exception) {
            val webIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
            ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            context.startActivity(webIntent)
        }
    }
}
