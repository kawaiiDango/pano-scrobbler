set JAVA_HOME=C:\jdks\jdk-21.0.2

call "%~dp0gradlew.bat" clean
call "%~dp0gradlew.bat" composeApp:exportLibraryDefinitions -PaboutLibraries.exportPath=src/commonMain/composeResources/files/ -PaboutLibraries.exportVariant=release
call "%~dp0gradlew.bat" fetchCrowdinMembers
call "%~dp0gradlew.bat" assembleReleaseGithub
call "%~dp0gradlew.bat" packageReleaseDistributionForCurrentOS
timeout /t 5
call "%~dp0gradlew.bat" clean

call wsl bash -ic "export JAVA_HOME=~/jdks/jdk-21.0.2/ && ./gradlew packageReleaseDistributionForCurrentOS"
