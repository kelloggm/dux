:: Ends the currently running Process Monitor instance after tracing is finished,
:: then converts the binary .pml log file into a .csv file
Procmon.exe /terminate
Procmon.exe /accepteula /quiet /minimized /openlog temp.pml /saveas strace.csv
del temp.pml