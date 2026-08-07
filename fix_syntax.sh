awk 'NR==1689 { print "    fun seekMedia(progress: Float) {" }
{ print $0 }' app/src/main/java/com/example/ui/HeadphoneViewModel.kt > tmp.kt
mv tmp.kt app/src/main/java/com/example/ui/HeadphoneViewModel.kt
