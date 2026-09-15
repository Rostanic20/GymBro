# WorkManager instantiates workers by class name.
-keep class hr.rostanic20.gymbro.data.backup.WeeklyBackupWorker { <init>(...); }

# Navigation 3 keys and the stored profile are serialized by kotlinx.serialization.
-keep class hr.rostanic20.gymbro.navigation.AppDestination$* { *; }
-keep class hr.rostanic20.gymbro.data.local.UserProfile { *; }
-keepclassmembers class hr.rostanic20.gymbro.** {
    *** Companion;
}
-keepclasseswithmembers class hr.rostanic20.gymbro.** {
    kotlinx.serialization.KSerializer serializer(...);
}
