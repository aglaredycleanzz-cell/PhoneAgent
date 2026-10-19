package io.phoneagent.mobile.data

import java.util.UUID

/**
 * Deterministic starter plan. This is a checklist template, not AI planning
 * and not a sequence of executable device commands.
 */
object TaskPlanFactory {
    fun create(rawRequest: String): List<TaskStep> {
        val request = normalizeTaskTitle(rawRequest).lowercase()
        val technical = listOf(
            "build", "gradle", "apk", "code", "github", "كود", "بناء",
            "خطأ", "برمج", "مشروع", "تطبيق", "ملف"
        ).any(request::contains)
        val communication = listOf(
            "send", "message", "email", "أرسل", "ارسل", "رسالة", "بريد"
        ).any(request::contains)

        val first = when {
            technical -> TaskStep(
                id = UUID.randomUUID().toString(),
                title = "تحديد المدخلات والمشكلة",
                detail = "تحديد ملفات المشروع أو رسالة الخطأ والنتيجة المطلوبة."
            )
            communication -> TaskStep(
                id = UUID.randomUUID().toString(),
                title = "مراجعة المحتوى والجهة",
                detail = "تأكيد نص الرسالة والجهة المقصودة قبل أي إرسال."
            )
            else -> TaskStep(
                id = UUID.randomUUID().toString(),
                title = "تحديد النتيجة المطلوبة",
                detail = "تحويل الطلب إلى نتيجة واضحة يمكن مراجعتها."
            )
        }

        val second = TaskStep(
            id = UUID.randomUUID().toString(),
            title = "مراجعة الصلاحيات والمخاطر",
            detail = "تحديد ما إذا كان الطلب يحتاج إذنًا أو موافقة إضافية."
        )
        val third = TaskStep(
            id = UUID.randomUUID().toString(),
            title = if (technical) "تحديد اختبار التحقق" else "اختيار الإجراء المسموح",
            detail = "صياغة معيار للنجاح؛ لا يوجد تنفيذ آلي في الإصدار الحالي."
        )

        return listOf(first, second, third)
    }
}
