# arity math library – used via direct API calls (Symbols, SyntaxException, Util)
-keep class org.javia.arity.** { *; }

# DataStore uses reflection for serialization
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite {
    <fields>;
}

# Keep line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
