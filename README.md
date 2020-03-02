### Kotlin / JNI Interface to Intel OIDN Image Denoise Library

[![Codeship Status for time4tea/oidnjni](https://app.codeship.com/projects/18071270-27e7-0138-360a-1a649adba10d/status?branch=master)](https://app.codeship.com/projects/383796)
![Bintray](https://img.shields.io/bintray/v/time4tea/oss/oidnjni)
![Maven Central](https://img.shields.io/maven-central/v/net.time4tea/oidnjni)

This is a work in progress, but does work!

These plots show the before and after for an image generated on a 
[toy raytracer](https://github.com/time4tea/raytrace-in-a-weekend-kotlin) 

The source image was generated using monte-carlo tracing with 100 samples/pixel. The scene renders in 34s.
This was then passed through the OIDN-JNI library - with the result as shown.

Its pretty good! - and is similar to outputs with many 1,000s of samples/pixel

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

## Show me the code

```kotlin
    val oidn = Oidn()
    val color = Oidn.allocateBuffer(image.width, image.height)
    // put some data in the image...
    val output = Oidn.allocateBuffer(image.width, image.height)

    oidn.newDevice(Oidn.DeviceType.DEVICE_TYPE_DEFAULT).use { device ->
        device.raytraceFilter().use { filter ->
            filter.setFilterImage(
                color, output, image.width, image.height
            )
            filter.commit()
            filter.execute()
            device.error()
        }
    }
```

## Performance

The code is almost certainly far from optimal, no effort has gone into making it performant. However, running the filter
 will dominate the performance of the code - the JNI wrapper is very thin.

Processing the above image takes 0.35s on my laptop.
