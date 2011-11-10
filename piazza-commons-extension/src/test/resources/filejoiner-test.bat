@echo off

%JAVA_HOME%\bin\java -cp ".;../;../lib/piazza-commons-ext.jar" hk.hku.cecid.piazza.commons.io.FileJoiner %1 %2 %3

PAUSE