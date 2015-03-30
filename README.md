# VirtualMachineManager

• Created VM Manager, output CPU (MHz) and memory (MB) usages of VMs and server to a txt file with a timestamp 

• Kept track of the server utilization file and migrated it if the average CPU or memory utilization exceeds a threshold set

VMM has the following functionalities:
===========================================================================

1. Every 15 seconds, the program outputs its CPU (MHz) and memory (MB) usages of your VMs
to a txt file with a timestamp.

2. Every 15 seconds, the program outputs the CPU (MHz) and memory (MB) usages of the server
where your VM runs to a txt file with a timestamp.

3. Keep track of the server utilization file and check if the average CPU OR memory utilization of
that server exceeds a threshold (set by you) every 3 minutes. If so, migrate your VM to another
server. 
