-keep class org.spongycastle.** { *; }
-dontwarn org.spongycastle.**

-keep class com.itextpdf.** { *; }

-keep class javax.xml.crypto.dsig.** { *; }
-dontwarn javax.xml.crypto.dsig.**

-keep class org.apache.jcp.xml.dsig.internal.dom.** { *; }
-dontwarn org.apache.jcp.xml.dsig.internal.dom.**

-keep class javax.xml.crypto.dom.** { *; }
-dontwarn javax.xml.crypto.dom.**

-keep class org.apache.xml.security.utils.** { *; }
-dontwarn org.apache.xml.security.utils.**

 -keep class com.itextpdf.** { *; }
   -dontwarn com.itextpdf.*

 -keep class javax.xml.crypto.dsig.** { *; }
 -dontwarn javax.xml.crypto.dsig.**
 -keep class javax.xml.crypto.** { *; }
 -dontwarn javax.xml.crypto.**
 -keep class org.spongycastle.** { *; }
 -dontwarn org.spongycastle.**