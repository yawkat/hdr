package at.yawk.hdr.index;

import at.yawk.hdr.HeapDumpUtil;
import at.yawk.hdr.PooledInputStream;
import at.yawk.hdr.StackTraceBuilder;
import at.yawk.hdr.StreamPool;
import at.yawk.hdr.format.*;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yawkat
 */
@Slf4j
public class Indexer {
    private final StreamPool pool;
    private final Executor executor;

    private final HprofDataReader reader = new HprofDataReader();

    private HprofFileHeader header;
    private int tagListStart;

    @Getter private final StringIndex stringIndex = new StringIndex();
    @Getter private final SerialIndex<HprofClassLoad> classSerialIndex = new SerialIndex<>();
    @Getter private final IdIndex<HprofClassLoad> classObjectIdIndex = new IdIndex<>();
    @Getter private final IdIndex<HprofStackFrame> stackFrameIndex = new IdIndex<>();
    @Getter private final SerialIndex<HprofStackTrace> stackTraceIndex = new SerialIndex<>();

    @Getter private final SerialIndex<StackData> threadIndex = new SerialIndex<>();
    @Getter private final IdIndex<TypeData> typeIndex = new IdIndex<>();
    private final TypeData[] primitiveArrayTypes = new TypeData[BaseType.MAX + 1];

    private final List<HeapDumpSegment> heapDumpSegments = new ArrayList<>();

    public Indexer(StreamPool pool, Executor executor) {
        this.pool = pool;
        this.executor = executor;
    }

    public ListenableFuture<?> execute(ThrowingRunnable r) {
        ListenableFutureTask<Void> future = new ListenableFutureTask<>(() -> {
            try {
                r.run();
            } catch (InterruptedException ignored) {
            } catch (Throwable e) {
                log.error("Error while executing task " + r, e);
            }
        }, null);
        executor.execute(future);
        return future;
    }

    /// root index ///

    public void scanRootIndex() throws IOException, InterruptedException {
        try (PooledInputStream stream = pool.open(0)) {
            header = reader.readHeader(stream);
            tagListStart = (int) stream.getPosition();

            if (log.isDebugEnabled()) {
                log.debug("File version: {}", header.version);
                log.debug("ID size: {} bytes", header.identifierSize);
                log.debug(
                        "Creation time: {} ({})",
                        header.time,
                        Instant.ofEpochMilli(header.time).atZone(ZoneId.systemDefault()).toString()
                );
            }
        }

        scanTags();
    }

    private void scanTags() throws IOException, InterruptedException {
        try (PooledInputStream stream = pool.open(tagListStart)) {
            HprofTagHeader tagHeader = new HprofTagHeader();

            while (!stream.hitEnd()) {
                reader.readTagHeader(stream, tagHeader);
                if (log.isTraceEnabled()) {
                    log.trace("Found tag of type 0x" + Integer.toHexString(tagHeader.type & 0xff) + " at " +
                              stream.getPosition());
                }
                switch (tagHeader.type) {
                case HprofString.ID:
                    HprofString string = new HprofString();
                    reader.readString(stream, string, tagHeader.length);
                    stringIndex.add(string);
                    break;
                case HprofClassLoad.ID:
                    HprofClassLoad classLoad = new HprofClassLoad();
                    reader.readClassLoad(stream, classLoad);
                    classSerialIndex.add(classLoad.serial, classLoad);
                    classObjectIdIndex.add(classLoad.objectId, classLoad);
                    break;
                case HprofStackFrame.ID:
                    HprofStackFrame stackFrame = new HprofStackFrame();
                    reader.readStackFrame(stream, stackFrame);
                    stackFrameIndex.add(stackFrame.id, stackFrame);
                    break;
                case HprofStackTrace.ID:
                    HprofStackTrace stackTrace = new HprofStackTrace();
                    reader.readStackTrace(stream, stackTrace);
                    stackTraceIndex.add(stackTrace.serial, stackTrace);
                    break;
                case HprofHeapDumpHeader.ID:
                case HprofHeapDumpSegmentHeader.ID:
                    heapDumpSegments.add(new HeapDumpSegment(stream.getPosition(), tagHeader.length));
                    // -- fall through: skip heap dump body
                default:
                    stream.seekBy(tagHeader.length);
                    break;
                }
            }
        }
        stringIndex.shrinkToContent(); // conserve unused memory
    }

    /// type index ///

    public void scanTypeIndex() throws IOException, InterruptedException {
        scanTypeIndex(ProgressCounter.EMPTY);
    }

    public void scanTypeIndex(ProgressCounter progressCounter) throws IOException, InterruptedException {
        walkHeapDump(new HeapDumpItemVisitor(true) {
            @Override
            public void visitRootJavaFrame(HprofHeapDumpRootJavaFrame item) {
                threadIndex.computeIfAbsent(item.threadSerial, StackData::new).objectCount.incrementAndGet();
            }

            @Override
            public boolean visitInstanceHeader(HprofHeapDumpInstanceHeader item) {
                TypeData typeData = typeIndex.computeIfAbsent(item.classObjectId, TypeData::new);
                typeData.instanceCount.incrementAndGet();
                typeData.memoryUsage.addAndGet(item.bodyLength);
                return false;
            }

            @Override
            public boolean visitObjectArrayDumpHeader(HprofHeapDumpObjectArrayDumpHeader item) {
                TypeData typeData = typeIndex.computeIfAbsent(item.classObjectId, TypeData::new);
                typeData.instanceCount.incrementAndGet();
                typeData.memoryUsage.addAndGet(item.length * header.identifierSize);
                return false;
            }

            @Override
            public boolean visitPrimitiveArrayDumpHeader(HprofHeapDumpPrimitiveArrayDumpHeader item) {
                TypeData arrayType = primitiveArrayTypes[item.elementType];
                if (arrayType == null) {
                    synchronized (primitiveArrayTypes) {
                        arrayType = primitiveArrayTypes[item.elementType];
                        if (arrayType == null) {
                            arrayType = new TypeData();
                            primitiveArrayTypes[item.elementType] = arrayType;
                        }
                    }
                }
                arrayType.instanceCount.incrementAndGet();
                arrayType.memoryUsage.addAndGet(item.length * BaseType.LENGTH[item.elementType]);
                return false;
            }

            @Override
            public void visitClassHeader(HprofHeapDumpClassHeader item) {
                TypeData typeData = typeIndex.computeIfAbsent(item.id, TypeData::new);

                typeData.classHeader = item.clone();
                typeData.classLoad = classObjectIdIndex.get(item.id);
                String descriptor = stringIndex.get(typeData.classLoad.nameId);
                typeData.name = HeapDumpUtil.pathToClassName(descriptor);
                typeData.fields = new ArrayList<>();
                byte primitiveArrayType = HeapDumpUtil.getPrimitiveArrayType(descriptor);
                if (primitiveArrayType != -1) {
                    typeData.arrayBaseType = primitiveArrayType;

                    synchronized (primitiveArrayTypes) {
                        TypeData previousType = primitiveArrayTypes[primitiveArrayType];
                        if (previousType != null) {
                            typeData.instanceCount.addAndGet(previousType.instanceCount.get());
                            typeData.memoryUsage.addAndGet(previousType.memoryUsage.get());
                        }
                        primitiveArrayTypes[primitiveArrayType] = typeData;
                    }
                } else if (descriptor.charAt(0) == '[') {
                    typeData.arrayBaseType = BaseType.OBJECT;
                } else {
                    typeData.arrayBaseType = -1;
                }
            }

            @Override
            public void visitField(HprofHeapDumpClassHeader classHeader, HprofHeapDumpClassField field) {
                typeIndex.computeIfAbsent(classHeader.id, TypeData::new).fields.add(field.clone());
            }

            @Override
            public void visitRootThreadObject(HprofHeapDumpRootThreadObject item) {
                StackData stackData = threadIndex.computeIfAbsent(item.threadSerial, StackData::new);
                stackData.threadObject = item.clone();
                StackTraceBuilder stackTraceBuilder = new StackTraceBuilder(Indexer.this);
                stackTraceBuilder.appendTrace(item.stackTraceSerial);
                stackData.name = item.threadSerial + "/" + stackTraceBuilder.summarize();
            }
        }, progressCounter).get();

        // build inherited field list
        for (TypeData typeData : typeIndex) {
            typeData.fieldsWithInherited = new ArrayList<>();
            TypeData current = typeData;
            while (current != null) {
                typeData.fieldsWithInherited.addAll(current.fields);
                current = typeIndex.get(current.classHeader.superClassObjectId);
            }
        }
    }

    /// walk heap dump ///

    public ListenableFuture<?> walkHeapDump(HeapDumpItemVisitor handler) {
        return walkHeapDump(handler, ProgressCounter.EMPTY);
    }

    public ListenableFuture<?> walkHeapDump(HeapDumpItemVisitor handler, ProgressCounter progressCounter) {
        progressCounter.setMax(heapDumpSegments.stream().mapToLong(s -> s.length).sum());

        if (handler.parallel) {
            Collection<ListenableFuture<?>> futures = new ArrayList<>();
            for (HeapDumpSegment segment : heapDumpSegments) {
                synchronized (segment) {
                    if (segment.currentEnqueuedVisitor != null) {
                        if (segment.currentEnqueuedVisitor instanceof MultiItemVisitor) {
                            ((MultiItemVisitor) segment.currentEnqueuedVisitor).add(handler);
                        } else {
                            segment.currentEnqueuedVisitor = new MultiItemVisitor(
                                    segment.currentEnqueuedVisitor,
                                    handler
                            );
                        }
                        if (progressCounter != ProgressCounter.EMPTY) {
                            if (segment.counter instanceof MultiProgressCounter) {
                                ((MultiProgressCounter) segment.counter).add(progressCounter);
                            } else {
                                segment.counter = new MultiProgressCounter(segment.counter, progressCounter);
                            }
                        }
                    } else {
                        ListenableFuture<?> future = execute(() -> {
                            HeapDumpItemVisitor visitor;
                            ProgressCounter counter;
                            synchronized (segment) {
                                visitor = segment.currentEnqueuedVisitor;
                                counter = segment.counter;
                                segment.currentEnqueuedVisitor = null;
                            }
                            walkHeapDumpSegment(segment, visitor, counter);
                        });
                        segment.currentEnqueuedVisitor = handler;
                        segment.consumerFuture = new MultiConsumerFuture<>(future);
                        segment.counter = progressCounter;
                    }
                    futures.add(segment.consumerFuture.openFuture());
                }
                futures.add(execute(() -> walkHeapDumpSegment(segment, handler, progressCounter)));
            }
            return new MultiFuture<>(futures);
        } else {
            return execute(() -> {
                for (HeapDumpSegment segment : heapDumpSegments) {
                    walkHeapDumpSegment(segment, handler, progressCounter);
                }
            });
        }
    }

    private void walkHeapDumpSegment(HeapDumpSegment segment, HeapDumpItemVisitor handler,
                                     ProgressCounter progressCounter)
            throws IOException, InterruptedException {
        log.debug("Walking heap dump segment {}", segment);
        try (PooledInputStream stream = pool.open(segment.position)) {
            long end = segment.position + segment.length;

            HprofHeapDumpClassHeader classHeader = new HprofHeapDumpClassHeader();
            HprofHeapDumpInstanceHeader instanceHeader = new HprofHeapDumpInstanceHeader();
            HprofHeapDumpObjectArrayDumpHeader objectArrayDumpHeader = new HprofHeapDumpObjectArrayDumpHeader();
            HprofHeapDumpPrimitiveArrayDumpHeader primitiveArrayDumpHeader =
                    new HprofHeapDumpPrimitiveArrayDumpHeader();
            HprofHeapDumpRootJavaFrame rootJavaFrame = new HprofHeapDumpRootJavaFrame();
            HprofHeapDumpRootJniGlobal rootJniGlobal = new HprofHeapDumpRootJniGlobal();
            HprofHeapDumpRootJniLocal rootJniLocal = new HprofHeapDumpRootJniLocal();
            HprofHeapDumpRootMonitorUsed rootMonitorUsed = new HprofHeapDumpRootMonitorUsed();
            HprofHeapDumpRootNativeStack rootNativeStack = new HprofHeapDumpRootNativeStack();
            HprofHeapDumpRootStickyClass rootStickyClass = new HprofHeapDumpRootStickyClass();
            HprofHeapDumpRootThreadBlock rootThreadBlock = new HprofHeapDumpRootThreadBlock();
            HprofHeapDumpRootThreadObject rootThreadObject = new HprofHeapDumpRootThreadObject();
            HprofHeapDumpRootUnknown rootUnknown = new HprofHeapDumpRootUnknown();

            HprofHeapDumpClassConstantPoolEntry constantPoolEntry = new HprofHeapDumpClassConstantPoolEntry();
            HprofHeapDumpClassStaticField staticField = new HprofHeapDumpClassStaticField();
            HprofHeapDumpClassField field = new HprofHeapDumpClassField();

            Thread currentThread = Thread.currentThread();

            long prevPos = stream.getPosition();
            while (!currentThread.isInterrupted()) {
                long posBefore = stream.getPosition();
                progressCounter.increment(posBefore - prevPos);
                if (posBefore >= end) {
                    break;
                }

                byte type = reader.readU1(stream);
                log.trace("Item {}", type);
                boolean skip = handler.maySkip(type);

                switch (type) {
                case HprofHeapDumpClassHeader.ID:
                    reader.readHeapDumpClassHeader(stream, classHeader);
                    handler.visitClassHeader(classHeader);

                    short constantPoolSize = reader.readU2(stream);
                    for (int i = 0; i < constantPoolSize; i++) {
                        reader.readHeapDumpClassConstantPoolEntry(stream, constantPoolEntry);
                        handler.visitConstantPoolEntry(classHeader, constantPoolEntry);
                    }

                    short staticFieldCount = reader.readU2(stream);
                    for (int i = 0; i < staticFieldCount; i++) {
                        reader.readHeapDumpClassStaticField(stream, staticField);
                        handler.visitStaticField(classHeader, staticField);
                    }

                    short fieldCount = reader.readU2(stream);
                    for (int i = 0; i < fieldCount; i++) {
                        reader.readHeapDumpClassField(stream, field);
                        handler.visitField(classHeader, field);
                    }
                    break;
                case HprofHeapDumpInstanceHeader.ID:
                    reader.readHeapDumpInstanceHeader(stream, instanceHeader);
                    boolean walk = handler.visitInstanceHeader(instanceHeader);
                    if (walk) {
                        List<HprofHeapDumpClassField> hierarchy =
                                typeIndex.get(instanceHeader.classObjectId).fieldsWithInherited;
                        for (HprofHeapDumpClassField decl : hierarchy) {
                            long val = reader.readPrimitive(stream, decl.type);
                            handler.visitInstanceField(instanceHeader, decl, val);
                        }
                        handler.visitInstanceEnd(instanceHeader);
                    } else {
                        stream.seekBy(instanceHeader.bodyLength);
                    }
                    break;
                case HprofHeapDumpObjectArrayDumpHeader.ID:
                    reader.readHeapDumpObjectArrayDumpHeader(stream, objectArrayDumpHeader);
                    walk = handler.visitObjectArrayDumpHeader(objectArrayDumpHeader);

                    if (walk) {
                        for (int i = 0; i < objectArrayDumpHeader.length; i++) {
                            long ref = reader.readId(stream);
                            handler.visitObjectArrayEntry(objectArrayDumpHeader, ref);
                        }
                        handler.visitObjectArrayEnd(objectArrayDumpHeader);
                    } else {
                        stream.seekBy(objectArrayDumpHeader.length * header.identifierSize);
                    }
                    break;
                case HprofHeapDumpPrimitiveArrayDumpHeader.ID:
                    reader.readHeapDumpPrimitiveArrayDumpHeader(stream, primitiveArrayDumpHeader);
                    walk = handler.visitPrimitiveArrayDumpHeader(primitiveArrayDumpHeader);

                    if (walk) {
                        for (int i = 0; i < primitiveArrayDumpHeader.length; i++) {
                            long value = reader.readPrimitive(stream, primitiveArrayDumpHeader.elementType);
                            handler.visitPrimitiveArrayEntry(value);
                        }
                        handler.visitPrimitiveArrayEnd(primitiveArrayDumpHeader);
                    } else {
                        stream.seekBy(primitiveArrayDumpHeader.length *
                                      BaseType.LENGTH[primitiveArrayDumpHeader.elementType]);
                        break;
                    }
                    break;
                case HprofHeapDumpRootJavaFrame.ID:
                    if (skip) {
                        stream.seekBy(header.identifierSize + 8);
                        break;
                    }

                    reader.readHeapDumpRootJavaFrame(stream, rootJavaFrame);
                    handler.visitRootJavaFrame(rootJavaFrame);
                    break;
                case HprofHeapDumpRootJniGlobal.ID:
                    if (skip) {
                        stream.seekBy(header.identifierSize * 2);
                        break;
                    }

                    reader.readHeapDumpRootJniGlobal(stream, rootJniGlobal);
                    handler.visitRootJniGlobal(rootJniGlobal);
                    break;
                case HprofHeapDumpRootJniLocal.ID:
                    if (skip) {
                        stream.seekBy(header.identifierSize + 8);
                        break;
                    }

                    reader.readHeapDumpRootJniLocal(stream, rootJniLocal);
                    handler.visitRootJniLocal(rootJniLocal);
                    break;
                case HprofHeapDumpRootMonitorUsed.ID:
                    if (skip) {
                        stream.seekBy(header.identifierSize);
                        break;
                    }

                    reader.readHeapDumpRootMonitorUsed(stream, rootMonitorUsed);
                    handler.visitRootMonitorUsed(rootMonitorUsed);
                    break;
                case HprofHeapDumpRootNativeStack.ID:
                    if (skip) {
                        stream.seekBy(header.identifierSize + 4);
                        break;
                    }

                    reader.readHeapDumpRootNativeStack(stream, rootNativeStack);
                    handler.visitRootNativeStack(rootNativeStack);
                    break;
                case HprofHeapDumpRootStickyClass.ID:
                    if (skip) {
                        stream.seekBy(header.identifierSize);
                        break;
                    }

                    reader.readHeapDumpRootStickyClass(stream, rootStickyClass);
                    handler.visitRootStickyClass(rootStickyClass);
                    break;
                case HprofHeapDumpRootThreadBlock.ID:
                    if (skip) {
                        stream.seekBy(header.identifierSize + 4);
                        break;
                    }

                    reader.readHeapDumpRootThreadBlock(stream, rootThreadBlock);
                    handler.visitRootThreadBlock(rootThreadBlock);
                    break;
                case HprofHeapDumpRootThreadObject.ID:
                    if (skip) {
                        stream.seekBy(header.identifierSize + 8);
                        break;
                    }

                    reader.readHeapDumpRootThreadObject(stream, rootThreadObject);
                    handler.visitRootThreadObject(rootThreadObject);
                    break;
                case HprofHeapDumpRootUnknown.ID:
                    if (skip) {
                        stream.seekBy(header.identifierSize);
                        break;
                    }

                    reader.readHeapDumpRootUnknown(stream, rootUnknown);
                    handler.visitRootUnknown(rootUnknown);
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported heap dump item id " + type);
                }

                prevPos = posBefore;
            }
            if (currentThread.isInterrupted()) { log.debug("Interrupted walking {}", segment); }
        }
    }

    /// getters ///

    public TypeData getPrimitiveArrayType(byte baseType) {
        return primitiveArrayTypes[baseType];
    }
}
