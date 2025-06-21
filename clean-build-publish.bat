set JAVA_HOME=%GRAALVM_HOME%

call "%~dp0gradlew.bat" clean
call "%~dp0gradlew.bat" composeApp:exportLibraryDefinitions -PaboutLibraries.exportVariant=releaseGithub
call "%~dp0gradlew.bat" composeApp:exportLibraryDefinitions -PaboutLibraries.exportVariant=release
call "%~dp0gradlew.bat" composeApp:exportLibraryDefinitions -PaboutLibraries.exportVariant=desktop
call "%~dp0gradlew.bat" composeApp:copyStringsToAndroid
call "%~dp0gradlew.bat" composeApp:fetchCrowdinMembers
call "%~dp0gradlew.bat" composeApp:assembleReleaseGithub
call "%~dp0gradlew.bat" composeApp:packageUberJarForCurrentOS
timeout /t 5
call "%~dp0gradlew.bat" clean

call wsl -e bash -ic "export JAVA_HOME=$GRAALVM_HOME && ./gradlew composeApp:packageUberJarForCurrentOS"
