# Scenevo ProGuard / R8
-keepattributes *Annotation*, InnerClasses, EnclosingMethod
-dontwarn javax.annotation.**
-keep class dagger.hilt.** { *; }
-keep class androidx.media3.** { *; }

# kotlinx.serialization — Project payload stored as JSON in Room
-keepattributes RuntimeVisibleAnnotations, AnnotationDefault
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class com.scenevo.domain.model.** { *; }
