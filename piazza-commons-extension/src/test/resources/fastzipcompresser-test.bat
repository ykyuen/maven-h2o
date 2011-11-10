@echo off

%JAVA_HOME%\bin\java -Xprof -cp ".;../;../lib/piazza-commons-ext.jar;../lib/piazza-commons.jar" hk.hku.cecid.piazza.commons.io.FastZIPCompresser

PAUSE