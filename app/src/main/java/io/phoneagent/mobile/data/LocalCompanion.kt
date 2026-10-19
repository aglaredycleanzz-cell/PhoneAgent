package io.phoneagent.mobile.data

/**
 * An honest offline conversation fallback for the UI prototype.
 *
 * This is deliberately rule-based, not an LLM. It can identify likely action
 * requests so the app can save them as drafts; it must never execute them.
 */
object LocalCompanion {
    private val actionPhrases = listOf(
        "افتح", "اضغط", "نفذ", "نفّذ", "شغل", "شغّل", "احذف",
        "أنشئ", "انشئ", "ابن", "ابني", "اصنع", "صلح", "أصلح",
        "افحص", "فحص", "ثبت", "ثبّت", "ارسل", "أرسل", "ابحث",
        "حلل", "حلّل", "برمج", "راجع", "نزّل", "نزل", "ارفع",
        "جهز", "جهّز", "ابدأ", "اربط", "اكتب", "صمم", "صمّم",
        "شغّل", "راقب", "حوّل", "حول", "build ", "open ", "run ",
        "fix ", "create ", "install ", "delete ", "check ", "write ",
        "deploy ", "send ", "connect "
    )

    fun isActionRequest(text: String): Boolean {
        val normalized = text.trim().lowercase()
        return normalized.length >= 4 && actionPhrases.any(normalized::contains)
    }

    fun reply(companionId: String, userText: String, taskWasSaved: Boolean): String {
        val normalized = userText.trim().lowercase()
        val personaLead = when (companionId) {
            "orion" -> "أوريون، المساعد التقني: "
            "sentinel" -> "سِنتِنل، مراجع الأمان: "
            else -> "نوفا، مخططة المهام: "
        }

        if (isGreeting(normalized)) {
            return personaLead +
                "أهلًا بك. يمكنك مناقشة هدفك معي أو إرسال طلب عملي. أعمل الآن بوضع محلي تجريبي؛ لا يوجد نموذج ذكاء اصطناعي متصل حتى الآن."
        }

        if (asksAboutCapabilities(normalized)) {
            return personaLead +
                "أستطيع في هذه النسخة حفظ محادثتنا محليًا، وتسجيل الطلبات العملية كمسودات في سجل النشاط. لا أستطيع بعد التحكم بالتطبيقات أو تنفيذ أوامر على الهاتف. ردودي الآن مبنية على قواعد محلية، وليست صادرة عن نموذج لغوي."
        }

        if (taskWasSaved) {
            val nextStep = when (companionId) {
                "orion" -> "الخطوة الهندسية التالية هي تحديد الأداة التي ستنفّذ الطلب وكيف سنختبر نتيجتها."
                "sentinel" -> "قبل أي تنفيذ مستقبلي، سنحدد الصلاحيات المطلوبة ونضيف معاينة وموافقة وإمكانية إلغاء."
                else -> "يمكنك متابعة النقاش أو مراجعة الطلب في تبويب النشاط. لم يحدث أي تنفيذ على الجهاز."
            }
            return personaLead +
                "التقطت طلبك وأضفته إلى سجل النشاط كمسودة محلية. محرك التحكم غير موصول بعد، لذلك لم أُنفّذه. " + nextStep
        }

        if (normalized.contains("صلاحية") || normalized.contains("أمان") || normalized.contains("خصوصية")) {
            return personaLead +
                "المبدأ الحالي هو عدم افتراض أي صلاحية: لا توجد خدمة تحكم أو اتصال شبكة مفعّل في هذا البناء. عند إضافة قدرة جديدة سنشرح ما تحتاجه ونترك تفعيلها لك."
        }

        if (normalized.contains("apk") || normalized.contains("بناء") || normalized.contains("خطأ")) {
            return personaLead +
                "لتحليل مشكلة بناء بدقة نحتاج سجل Gradle أو رسالة الخطأ كاملة. هذه النسخة لا تتصل بالمشروع تلقائيًا؛ أرسل الخطأ هنا وسأساعدك في تحديد الملف والخطوة المناسبة."
        }

        val roleAdvice = when (companionId) {
            "orion" -> "لنحوّل الفكرة إلى مشكلة محددة: ما النتيجة المطلوبة، وما الشيء الذي يجب أن يتغير، وكيف سنعرف أن الحل نجح؟"
            "sentinel" -> "قبل أن نقرر، لنحدد ما الذي قد يتغير، وما البيانات التي نحتاجها، وكيف نلغي الإجراء إذا لم تكن النتيجة صحيحة."
            else -> "لنبدأ بالنتيجة التي تريد الوصول إليها، ثم نقسمها إلى خطوات صغيرة قابلة للمراجعة."
        }

        return personaLead + roleAdvice +
            " ملاحظة: هذا رد محلي مبني على قواعد بسيطة؛ المحادثة الذكية الكاملة لم تُربط بعد."
    }

    private fun isGreeting(text: String): Boolean =
        listOf("مرحبا", "مرحبًا", "السلام عليكم", "اهلا", "أهلا", "هلا", "hello", "hi ")
            .any(text::startsWith)

    private fun asksAboutCapabilities(text: String): Boolean =
        listOf("ماذا تستطيع", "ماذا يمكنك", "وش تقدر", "ما الذي تستطيع", "ما الذي يمكنك", "قدراتك", "what can you", "help")
            .any(text::contains)
}
