package gamalsolutions.gamalprojects

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MainActivityStartupTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun appStartsNavigatesTabsAndAddsOrganizedContactWithoutCrashing() {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("splash_screen").fetchSemanticsNodes().isEmpty()
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("add_contact_fab").assertExists()
        composeRule.onNodeWithTag("filter_platform_whatsapp").assertExists()
        composeRule.onNodeWithTag("filter_platform_telegram").performScrollTo().assertExists()

        // الانتقال إلى تبويب الاستيراد والتصدير والتحقق من خيارات التنظيم
        composeRule.onNodeWithTag("nav_tab_transfer").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("quick_export_whatsapp_button").assertExists()
        composeRule.onNodeWithTag("import_export_lazy_column").performScrollToIndex(1)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("import_opt_platform_telegram").performScrollTo().assertExists()
        composeRule.onNodeWithTag("pick_vcf_file_button").assertExists()

        // الانتقال إلى تبويب الإعدادات والإحصائيات
        composeRule.onNodeWithTag("nav_tab_settings").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("create_backup_button").assertExists()

        // العودة إلى قائمة جهات الاتصال وفتح شاشة الإضافة
        composeRule.onNodeWithTag("nav_tab_contacts").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("add_contact_fab").performClick()
        composeRule.waitForIdle()

        // إدخال جهة اتصال مع تحديد تصنيف ومنصة وحفظها
        composeRule.onNodeWithTag("contact_name_input").performTextInput("محمد أحمد")
        composeRule.onNodeWithTag("contact_phone_input").performTextInput("+966501234567")
        composeRule.onNodeWithTag("edit_category_clients").performScrollTo().performClick()
        composeRule.onNodeWithTag("edit_platform_whatsapp").performScrollTo().performClick()
        composeRule.onNodeWithTag("save_contact_button").performScrollTo().performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("add_contact_fab").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("add_contact_fab").assertExists()
    }
}
