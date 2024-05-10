@echo off

REM Declaration des variables
    set nom_projet=temp
    set temp=C:\Users\ME-PC\Documents\GitHub\test\temp
    set web=C:\Users\ME-PC\Documents\GitHub\test\web
    set xml=C:\Users\ME-PC\Documents\GitHub\test\xml
    set lib=C:\Users\ME-PC\Documents\GitHub\test\lib
    set war=.\
    set src=C:\Users\ME-PC\Documents\GitHub\test\src

REM Suppression de temp si il existe
    rmdir /s /q %temp%

REM Creation du nouveau temp
    mkdir %temp%
    REM Creation du sous-repertoire temp/WEB-INF/lib
        mkdir %temp%"\WEB-INF\lib" 
    REM Creation du sous-repertoire temp/WEB-INF/classes
        mkdir %temp%"\WEB-INF\classes"

REM Copie des web de notre espace de travail initial vers le repertoire temporaire temp
    xcopy /s /e /q %web% %temp%

REM Copie du web.xml de notre espace de travail initial vers temp/WEB-INF/
    xcopy %xml% %temp%"\WEB-INF\"

REM Copie des *.jar de notre espace de travail initial ver temp/WEB-INF/lib
    xcopy /s /e /q %lib% %temp%"\WEB-INF\lib"

REM compilation du code source
    for /r "%src%" %%i in (*.java) do (
        javac -cp "%lib%\*;" -sourcepath %src% -d %temp%"\WEB-INF\classes" "%%i"
    )

REM Convertir le repertoire temp en .war
    jar -cf %nom_projet%.war -C %temp% .

    @REM rmdir /s /q %temp%

REM Copie du fichier war vers tomcat/webapps
    xcopy /y %nom_projet%.war "C:\Program Files\Apache Software Foundation\Tomcat 10.1\webapps"

REM Supprimer le fichier WAR temporaire
    del %nom_projet%.war

