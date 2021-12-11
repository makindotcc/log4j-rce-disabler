# log4j-rce-disabler

Static java agent that disables text substitution by default. \
\
Add the -javaagent parameter at the beginning to use this agent.
Example:
```
java -javaagent:Log4jCveFix.jar -jar application.jar
```
