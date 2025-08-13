@echo off
set JAVA_TOOL_OPTIONS=-XX:StartFlightRecording=filename=recording.jfr,duration=60s
call gradlew.bat run
pause