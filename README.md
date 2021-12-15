# log4j-rce-disabler

Static java agent that disables text substitution by **default**.
### Warning
It is still possible to create a logger instance with text parsing enabled.
This agent disables tag matching for the default [StrSubstitutor](https://github.com/apache/logging-log4j2/blob/44569090f1cf1e92c711fb96dfd18cd7dccc72ea/log4j-core/src/main/java/org/apache/logging/log4j/core/lookup/StrSubstitutor.java#L151).
The best way to be sure it is secure is to simply test it yourself using [nc(1)](https://man.openbsd.org/nc.1). \
Log text ``${jndi:ldap://server_ip_with_nc_active:nc_port/a}`` and check if [nc(1)](https://man.openbsd.org/nc.1) received a connection from your application.
If yes then it means your program is still vulnerable.

## Usage
Add the -javaagent parameter at the beginning to use this agent.
Example:
```
java -javaagent:Log4jCveFix.jar -jar application.jar
```
### Features
- disable lookup support in Log4j 
- halt when something tried to initialize `com/sun/jndi/ldap/Connection`
