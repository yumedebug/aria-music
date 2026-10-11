# Media3 sessions are instantiated reflectively by the media session framework.
-keep class androidx.media3.session.** { *; }
-dontwarn androidx.media3.**

# NewPipeExtractor (YouTube の署名解除) が jsoup 経由で参照する任意依存。
# 実行時には使われないため R8 の警告を抑制する。
-dontwarn com.google.re2j.Matcher
-dontwarn com.google.re2j.Pattern
-dontwarn java.beans.BeanDescriptor
-dontwarn java.beans.BeanInfo
-dontwarn java.beans.IntrospectionException
-dontwarn java.beans.Introspector
-dontwarn java.beans.PropertyDescriptor
-dontwarn javax.script.ScriptEngineFactory
