# ابدأ من هنا — Guardexa

## الحالة الواقعية للمشروع

هذه الحزمة تجمع الشيفرة التي أنشأناها في الحزم 1–28 داخل مشروع Android متعدد الوحدات.
هي **نسخة تطوير Alpha** وليست APK نهائيًا، ولم يتم تشغيل Gradle أو Android Emulator داخل هذه الجلسة.

الأشياء الجاهزة:
- هيكل Clean Architecture متعدد الوحدات.
- Gradle وVersion Catalog.
- واجهات Compose الأساسية.
- Room entities وDAOs.
- Policy Engine والمهلة وفقدان الوجه.
- CameraX pipeline.
- MediaPipe وTFLite adapters.
- PIN وKeystore.
- Safe Mode وWatchdog.
- الوقت والجداول والتقارير.
- Manifest وقواعد النسخ الاحتياطي.
- اختبارات Unit أولية.

الأشياء التي تحتاج اللابتوب:
- أول Gradle Sync وإصلاح أخطاء التكامل التي تظهر.
- إضافة النموذجين الحقيقيين.
- ربط ViewModels الفعلية بدل بعض bindings التجريبية في MainActivity.
- اختبار Foreground Service وDevice Owner على هاتف حقيقي.
- تدريب/اختيار نموذج نظارات موثوق.
- تحسين الترجمات والموارد.
- اختبار استهلاك البطارية والحرارة.

## البرامج المطلوبة

1. Android Studio مستقر حديث.
2. JDK 17 المدمج مع Android Studio.
3. Android SDK Platform 36.
4. Android SDK Build Tools.
5. Git، اختياري لكنه موصى به.

## خطوات الفتح

1. فك ضغط Guardexa_Final_Project.zip.
2. افتح مجلد Guardexa_Final_Project في Android Studio.
3. أنشئ local.properties من local.properties.example.
4. دع Android Studio ينفذ Gradle Sync.
5. شغّل أولًا:
   ./gradlew assembleStandardDebug
6. ثم:
   ./gradlew testStandardDebugUnitTest
7. لا تختبر AI قبل إضافة النموذجين الحقيقيين.

## أول الأخطاء المتوقعة

لأن الشيفرة جُمعت من حزم مستقلة ولم تُبنَ داخل Android Studio بعد، قد تظهر:
- تبعية Module ناقصة.
- Import يحتاج تعديلًا.
- API تغيّر في MediaPipe أو TensorFlow Lite.
- Hilt binding ناقص لخدمة أو Receiver.
- تعارض في أسماء أو DAO أثناء الدمج.
- تحذير/خطأ متعلق بسياسات Foreground Service على نسخة Android المستهدفة.

هذه أخطاء تكامل متوقعة في أول Build، وليست دليلًا على فقدان المشروع.

## الترتيب الصحيح للإصلاح

1. Gradle وVersion Catalog.
2. Compilation errors في core-model/core-domain.
3. Room وKSP.
4. Hilt.
5. Compose.
6. CameraX.
7. MediaPipe.
8. TFLite.
9. Foreground Service.
10. Device Owner على جهاز اختبار منفصل.

## تنبيه أمان

لا تستخدم Device Owner على هاتفك الشخصي أثناء التجارب الأولى.
استخدم هاتف اختبار قابلًا لإعادة الضبط، لأن إزالة Device Owner قد تتطلب إعادة ضبط المصنع حسب طريقة provisioning.
Guardexa نفسه لا ينفذ أو يمنع إعادة ضبط المصنع.
