:: REM Launches Process Monitor to begin recording system events.
start Procmon.exe /accepteula /quiet /minimized /backingfile temp.pml /LoadConfig src\org\dux\stracetool\--Detail++ParentPid.pmc
Procmon.exe /waitforidle