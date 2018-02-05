:: REM Launches Process Monitor to begin recording system events.
start Procmon.exe /accepteula /quiet /minimized /backingfile temp.pml
Procmon.exe /waitforidle