# modbus-javacomm
Fork of Jamod (Java Modbus) using purejavacomm instead of native rxtx

This is a fork of [Jamod](http://jamod.sourceforge.net) with a little modification:
It replaces the classes for serial connection from the sun/oracle (the package "javax.comm.*") with the implementation from the [purejavacomm](https://github.com/nyholku/purejavacomm) project.
Therefore, it is not needed to add native rxtx driver libraries for using the jamod! Look also at the NOTICE for their licenses!

# Usage
## Maven

```xml
<dependency>
  <groupId>de.bluebiz</groupId>
  <artifactId>modbus-javacomm</artifactId>
  <version>1.5</version>
</dependency>
```

