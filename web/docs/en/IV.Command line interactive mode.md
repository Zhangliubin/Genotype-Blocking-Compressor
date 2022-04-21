The input parameters of command line interaction mode are the same as those of command line mode, but all the commands are in the same process, which will reduce the time spent on JVM startup and just-in-time compilation (JIT). 

When you type a command, you no longer need to specify `java -Xmx4g -Xms4g -jar gbc.jar`. The command line interaction mode has four additional parameters in addition to the parameters in command line mode:

| Parameters             | Description                                                  |
| ---------------------- | ------------------------------------------------------------ |
| exit, q, quit          | Exit program, exit the command line interaction mode.        |
| clear                  | Clearing the screen (actually printing out multiple blank lines). |
| reset                  | Clear the data buffer.                                       |
| Lines begin with a "#" | For annotation.                                              |
