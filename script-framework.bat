@echo off

REM Declaration des variables
    set nom_projet=framework
    set lib=C:\Users\ME-PC\Documents\GitHub\lib
    set src=C:\Users\ME-PC\Documents\GitHub\main\src
    set temp=C:\Users\ME-PC\Documents\GitHub\temp

    mkdir %temp%
REM compilation du code source
    for /r "%src%" %%i in (*.java) do (
        javac -cp "%lib%\*;" -sourcepath %src% -d %temp% "%%i"
    )

REM Convertir le repertoire temp en .jar
    jar -cf %nom_projet%.jar -C %temp% .

    rmdir %temp%

xcopy /y %nom_projet%.jar "C:\Users\ME-PC\Documents\GitHub\test\lib"
