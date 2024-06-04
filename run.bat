@echo off

REM Répertoire contenant vos fichiers .java
set "JAVA_DIR=.\src"

REM Répertoire contenant vos fichiers .class
set "CLASSES_DIR=classes"

REM Répertoire contenant les fichiers JAR
set "LIB_DIR=.\lib"

REM Nom de votre fichier JAR
set "JAR_FILE=framework.jar"

REM Compilation en une seule passe
javac -d %CLASSES_DIR% -cp "%LIB_DIR%\*" %JAVA_DIR%\annot\*.java %JAVA_DIR%\utilities\*.java %JAVA_DIR%\controllers\*.java

REM Création du fichier JAR
jar cvf %JAR_FILE% -C %CLASSES_DIR% .

REM Supprimer le dossier contenant les .classes (décommenter si nécessaire)
rmdir /s /q %CLASSES_DIR%

REM couper les fichiers .jar vers test
xcopy /s /q /y "framework.jar" "C:\Users\NJAKATIANA\Desktop\Naina\framework\sprint4-test\lib"