### Kotlin / JNI Interface to Intel OIDN Image Denoise Library

This is a work in progress, but does work!

## Before

![Example](oidnkt/src/test/resources/weekfinal.png)

## After

![Example](example-output/weekfinal.png)


## How to build

Right now, you'll need to compile the kotlin and C library 

```$bash
./gradlew assemble
```

This will create `oidnkt/build/libs/oidnkt-1.0-SNAPSHOT.jar` and `oidnjni/build/oidnlib/liboidnjni.so`

## How to use

You can look at the `OidnTest` class to see how to use the library - it requires images in a float format.

You'll need to set `java.library.path` to find the oidnjni.so library, and *also* LD_LIBRARY_PATH so that the 
dependencies can be found. The gradle build does this for you when running the tests...

