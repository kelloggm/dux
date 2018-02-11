:: Ends the currently running Process Monitor instance after tracing is finished,
:: then converts the binary .pml log file into a .csv file
Procmon.exe /terminate
Procmon.exe /accepteula /quiet /minimized /openlog temp.pml /saveas strace.csv
move strace.csv trace.out
del temp.pml
del strace.csv