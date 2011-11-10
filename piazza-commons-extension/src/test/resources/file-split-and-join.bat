@echo off

if "%1" == "" goto USAGE

%JAVA_HOME%\bin\java -cp ".;../;../dist/lib/LFTv0.1.jar" hk.hku.cecid.piazza.commons.io.FileSplitter %1 %2 %3
%JAVA_HOME%\bin\java -cp ".;../;../dist/lib/LFTv0.1.jar" hk.hku.cecid.piazza.commons.io.FileJoiner %4 %2 %5

:USAGE
echo Usage: file-split-and-join.bat [f] [o] [segmentsize] [r] [deleteSegment]
echo [f]            - The filepath to split.
echo [o]            - The output path of splitted file / lookup path for the joiner
echo [segmentsize]  - The segment size in bytes
echo [r]            - The joined output files.
echo [deleteSegment]- The flag indicating whether delete the files segment when joining has been finished [true, false]
echo Example:
echo file-split-and-join.bat ../testdata-input/test.gif ../testdata-output/ 1024 ../testdata-result/test.gif true

PAUSE