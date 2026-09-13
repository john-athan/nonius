# Nothing in this app is reached by reflection, so R8 may shrink all of it.
# Line numbers stay, because a stack trace from a user is the only bug report
# an offline app can produce.
-keepattributes SourceFile,LineNumberTable
