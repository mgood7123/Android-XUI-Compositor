MAUI's JAVAC cannot use the following:

 - androidx.annotations.NonNull
 - androidx.annotations.Nullable

 - Lambda
  - () -> foo
   - use intellij (Android Studio) to "convert to new Anonymous Class"
  - this::method
   - use intellij (Android Studio) to "convert to Lambda" -> "convert to new Anonymous Class"
  - field::method
   - use intellij (Android Studio) to "convert to Lambda" -> "convert to new Anonymous Class"

 - wait/notify method names
  - these are used by C# Java.Lang.Object
