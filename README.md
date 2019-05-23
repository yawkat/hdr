HDR
===

Java heap dump (HProf format) reader with low memory requirements.

core
----

The core module contains headless utilities for low-memory operations on heap dumps:

- parsing heap dumps
- scanning for objects and references

Example usage:

```
StreamPool file = new MemoryCachedFilePool(new File("dump.hprof"));
Executor executor = Executors.newFixedThreadPool(4);

Indexer indexer = new Indexer(file, executor);

// scan root index (strings, where the actual heap dump is etc)
indexer.scanRootIndex();
// scan the heap dump data found above once to index classes
indexer.scanTypeIndex();

indexer.walkHeapDump(new HeapDumpItemVisitor(true) {
    @Override
    public boolean visitInstanceHeader(HprofHeapDumpInstanceHeader item) {
        TypeData type = indexer.getTypeIndex().get(item.classObjectId);
        System.out.println("Found object of type " + type.getName());
        return false;
    }
});
```

gui
---

This module contains a javafx GUI for analyzing heap dumps. Memory usage goals are ~1/3rd of the heap dump size and
this works fairly well already (Xmx 3GB, 8GB heap dump):

![Initial scan screen](https://github.com/yawkat/hdr/blob/screenshots/7XVg.png?raw=true)

![Top object view](https://github.com/yawkat/hdr/blob/screenshots/qmk3.png?raw=true)

![Thread view](https://github.com/yawkat/hdr/blob/screenshots/rq82.png?raw=true)

![Type reference view](https://github.com/yawkat/hdr/blob/screenshots/S0ps.png?raw=true)

![Thread details (WIP)](https://github.com/yawkat/hdr/blob/screenshots/oYRc.png?raw=true)

JPMS
----

To run on Java 9+, you'll need to configure the JVM to allow access to JavaFX modules:

```
--add-opens
javafx.graphics/com.sun.javafx.util=ALL-UNNAMED
--add-opens
javafx.base/com.sun.javafx.reflect=ALL-UNNAMED
--add-opens
javafx.base/com.sun.javafx.beans=ALL-UNNAMED
--add-opens
javafx.graphics/com.sun.javafx.scene.layout=ALL-UNNAMED
--add-opens
javafx.graphics/com.sun.javafx.application=ALL-UNNAMED
--add-opens
javafx.graphics/com.sun.javafx.tk=ALL-UNNAMED
--add-opens
javafx.graphics/com.sun.javafx.scene.text=ALL-UNNAMED
--add-opens
javafx.graphics/com.sun.javafx.scene=ALL-UNNAMED
--add-opens
javafx.base/com.sun.javafx.logging=ALL-UNNAMED
--add-opens
javafx.base/com.sun.javafx.collections=ALL-UNNAMED
--add-opens
javafx.base/com.sun.javafx.event=ALL-UNNAMED
--add-opens
javafx.graphics/com.sun.javafx.scene.traversal=ALL-UNNAMED
--add-opens
javafx.graphics/com.sun.javafx.stage=ALL-UNNAMED
--add-opens
javafx.graphics/com.sun.javafx.geom=ALL-UNNAMED
--add-opens
javafx.base/com.sun.javafx=ALL-UNNAMED
```
