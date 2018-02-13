# Strace Tool

The Strace Tool library can be used to abstract the functionality of a
system-call tracing utility (such as strace on Linux) across platforms*.

There are multiple entry points into the library, depending on your
project needs:

> // Running strace on a specified executable, and dumping output to a log file.\
> Tracer t  = new Tracer("additional args to strace");\
> t.trace();

> // Parsing an existing strace-like log file.\
>List\<StraceCall> StraceParser.parse("path to strace log file") \

Note: Windows users will need to install Process Monitor, a free download from
Microsoft here: https://docs.microsoft.com/en-us/sysinternals/downloads/procmon .
Process Monitor is an advanced system monitoring tool, which (among other
features), can trace a process' system calls. Process Monitor is in no way 
affiliated with this project and its use is governed by its own separate EULA.

\* Currently only Linux (e.g. CentOS) and Windows (kind of).