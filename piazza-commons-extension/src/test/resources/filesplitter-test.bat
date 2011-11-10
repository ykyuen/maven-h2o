@echo off

%JAVA_HOME%\bin\java -cp ".;../;../lib/piazza-commons-ext.jar;../lib/piazza-commons.jar" hk.hku.cecid.piazza.commons.io.FileSplitter %1 %2 %3 %4 %5 %6

PAUSE