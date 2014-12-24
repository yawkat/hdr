package at.yawk.hdr.format;

import at.yawk.hdr.PooledInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @author yawkat
 */
public class HprofDataReader {
    /**
     * Size of an id element in bytes.
     */
    private int idSize = -1;

    public void setIdSize(int idSize) {
        this.idSize = idSize;
        if (idSize > 8) { throw new UnsupportedOperationException("Unsupported ID size " + idSize); }
    }

    public byte readU1(PooledInputStream stream) throws IOException {
        return (byte) stream.read();
    }

    public short readU2(PooledInputStream stream) throws IOException {
        byte[] read = stream.read(2);
        return (short) ((read[0] & 0xff) << 8 | read[1] & 0xff);
    }

    public int readU4(PooledInputStream stream) throws IOException {
        byte[] read = stream.read(4);
        return (read[0] & 0xff) << 24 |
               (read[1] & 0xff) << 16 |
               (read[2] & 0xff) << 8 |
               read[3] & 0xff;
    }

    public float readF4(PooledInputStream stream) throws IOException {
        return Float.intBitsToFloat(readU4(stream));
    }

    public long readU8(PooledInputStream stream) throws IOException {
        byte[] read = stream.read(8);
        return (read[0] & 0xffL) << 56 |
               (read[1] & 0xffL) << 48 |
               (read[2] & 0xffL) << 40 |
               (read[3] & 0xffL) << 32 |
               (read[4] & 0xff) << 24 |
               (read[5] & 0xff) << 16 |
               (read[6] & 0xff) << 8 |
               read[7] & 0xff;
    }

    public long readId(PooledInputStream stream) throws IOException {
        byte[] read = stream.read(idSize);
        long v = 0;
        for (int i = 0; i < idSize; i++) {
            v <<= 8;
            v |= read[i] & 0xff;
        }
        return v;
    }

    /**
     * Read a header from a given stream. Also sets this reader's ID size to the one stored in the header.
     */
    public void readHeader(PooledInputStream stream, HprofFileHeader target) throws IOException {
        StringBuilder builder = new StringBuilder();
        int next;
        while ((next = readU1(stream)) != 0) {
            builder.append((char) next);
        }

        target.version = builder.toString();
        target.identifierSize = readU4(stream);
        target.time = readU8(stream);

        setIdSize(target.identifierSize);
    }

    public HprofFileHeader readHeader(PooledInputStream stream) throws IOException {
        HprofFileHeader header = new HprofFileHeader();
        readHeader(stream, header);
        return header;
    }

    public void readTagHeader(PooledInputStream stream, HprofTagHeader header) throws IOException {
        header.type = readU1(stream);
        header.timeOffset = readU4(stream);
        header.length = readU4(stream);
    }

    public void readAllocSitesEntry(PooledInputStream stream, HprofAllocSitesEntry target) throws IOException {
        target.arrayType = readU4(stream);
        target.classSerial = readU4(stream);
        target.stackTraceSerial = readU4(stream);
        target.liveBytes = readU4(stream);
        target.liveInstances = readU4(stream);
        target.allocatedBytes = readU4(stream);
        target.allocatedInstances = readU4(stream);
    }

    public void readAllocSitesHeader(PooledInputStream stream, HprofAllocSitesHeader target) throws IOException {
        target.flags = readU2(stream);
        target.cutoffRatio = readF4(stream);
        target.liveBytes = readU4(stream);
        target.liveInstances = readU4(stream);
        target.allocatedBytes = readU8(stream);
        target.entryCount = readU4(stream);
    }

    public void readControlSettings(PooledInputStream stream, HprofControlSettings target) throws IOException {
        target.flags = readU4(stream);
        target.stackTraceDepth = readU2(stream);
    }

    public void readCpuSamplesEntry(PooledInputStream stream, HprofCpuSamplesEntry target) throws IOException {
        target.sampleCount = readU4(stream);
        target.stackTraceSerial = readU4(stream);
    }

    public void readCpuSamplesEntry(PooledInputStream stream, HprofCpuSamplesHeader target) throws IOException {
        target.sampleCount = readU4(stream);
    }

    public void readClassLoad(PooledInputStream stream, HprofClassLoad target) throws IOException {
        target.serial = readU4(stream);
        target.objectId = readId(stream);
        target.stackSerial = readU4(stream);
        target.nameId = readId(stream);
    }

    public void readClassUnload(PooledInputStream stream, HprofClassUnload target) throws IOException {
        target.serial = readU4(stream);
    }

    public void readStackFrame(PooledInputStream stream, HprofStackFrame target) throws IOException {
        target.id = readId(stream);
        target.methodNameId = readId(stream);
        target.methodSignatureId = readId(stream);
        target.sourceFileNameId = readId(stream);
        target.classSerial = readU4(stream);
        target.lineNumber = readU4(stream);
    }

    public void readStackTrace(PooledInputStream stream, HprofStackTrace target) throws IOException {
        target.serial = readU4(stream);
        target.threadSerial = readU4(stream);
        int frameCount = readU4(stream);
        target.frameIds = new long[frameCount];
        for (int i = 0; i < frameCount; i++) {
            target.frameIds[i] = readId(stream);
        }
    }

    public void readString(PooledInputStream stream, HprofString target, int bodyLength) throws IOException {
        target.id = readId(stream);
        byte[] data = stream.read(bodyLength - idSize);
        target.value = new String(data, StandardCharsets.UTF_8);
    }

    public void readThreadStart(PooledInputStream stream, HprofThreadStart target) throws IOException {
        target.serial = readU4(stream);
        target.objectId = readId(stream);
        target.stackTraceSerial = readU4(stream);
        target.threadNameId = readId(stream);
        target.threadGroupNameId = readId(stream);
        target.threadGroupParentNameId = readId(stream);
    }

    public void readThreadEnd(PooledInputStream stream, HprofThreadEnd target) throws IOException {
        target.serial = readU4(stream);
    }

    public void readHeapDumpClassConstantPoolEntry(PooledInputStream stream, HprofHeapDumpClassConstantPoolEntry target)
            throws IOException {
        target.index = readU2(stream);
        target.type = readU1(stream);
        target.value = readPrimitive(stream, target.type);
    }

    public long readPrimitive(PooledInputStream stream, byte type) throws IOException {
        switch (type) {
        case BaseType.BOOLEAN:
        case BaseType.BYTE:
            return readU1(stream);
        case BaseType.SHORT:
        case BaseType.CHAR:
            return readU2(stream);
        case BaseType.INT:
        case BaseType.FLOAT:
            return readU4(stream);
        case BaseType.LONG:
        case BaseType.DOUBLE:
            return readU8(stream);
        case BaseType.OBJECT:
            return readId(stream); // TODO: confirm that this is the correct action for objects
        default:
            throw new UnsupportedOperationException("Unsupported primitive type " + type);
        }
    }

    public void readHeapDumpClassField(PooledInputStream stream, HprofHeapDumpClassField target) throws IOException {
        target.nameId = readId(stream);
        target.type = readU1(stream);
    }

    public void readHeapDumpClassStaticField(PooledInputStream stream, HprofHeapDumpClassStaticField target)
            throws IOException {
        target.nameId = readId(stream);
        target.type = readU1(stream);
        target.value = readPrimitive(stream, target.type);
    }

    public void readHeapDumpClassHeader(PooledInputStream stream, HprofHeapDumpClassHeader target) throws IOException {
        target.id = readId(stream);
        target.stackTraceSerial = readU4(stream);
        target.superClassObjectId = readId(stream);
        target.classLoaderObjectId = readId(stream);
        target.signersObjectId = readId(stream);
        target.protectionDomainObjectId = readId(stream);
        target.reserved1 = readId(stream);
        target.reserved2 = readId(stream);
        target.instanceSize = readU4(stream);
    }

    public void readHeapDumpInstanceHeader(PooledInputStream stream, HprofHeapDumpInstanceHeader target)
            throws IOException {
        target.id = readId(stream);
        target.stackTraceSerial = readU4(stream);
        target.classObjectId = readId(stream);
        target.bodyLength = readU4(stream);
    }

    public void readHeapDumpObjectArrayDumpHeader(PooledInputStream stream, HprofHeapDumpObjectArrayDumpHeader target)
            throws IOException {
        target.id = readId(stream);
        target.stackTraceSerial = readU4(stream);
        target.length = readU4(stream);
        target.classObjectId = readId(stream);
    }

    public void readHeapDumpPrimitiveArrayDumpHeader(PooledInputStream stream, HprofHeapDumpPrimitiveArrayDumpHeader
            target)
            throws IOException {
        target.id = readId(stream);
        target.stackTraceSerial = readU4(stream);
        target.length = readU4(stream);
        target.elementType = readU1(stream);
    }

    public void readHeapDumpRootJavaFrame(PooledInputStream stream, HprofHeapDumpRootJavaFrame target)
            throws IOException {
        target.id = readId(stream);
        target.threadSerial = readU4(stream);
        target.stackTraceFrameIndex = readU4(stream);
    }

    public void readHeapDumpRootJniGlobal(PooledInputStream stream, HprofHeapDumpRootJniGlobal target)
            throws IOException {
        target.id = readId(stream);
        target.refId = readId(stream);
    }

    public void readHeapDumpRootJniLocal(PooledInputStream stream, HprofHeapDumpRootJniLocal target)
            throws IOException {
        target.id = readId(stream);
        target.threadSerial = readU4(stream);
        target.stackTraceFrame = readU4(stream);
    }

    public void readHeapDumpRootMonitorUsed(PooledInputStream stream, HprofHeapDumpRootMonitorUsed target)
            throws IOException {
        target.objectId = readId(stream);
    }

    public void readHeapDumpRootNativeStack(PooledInputStream stream, HprofHeapDumpRootNativeStack target)
            throws IOException {
        target.id = readId(stream);
        target.threadSerial = readU4(stream);
    }

    public void readHeapDumpRootStickyClass(PooledInputStream stream, HprofHeapDumpRootStickyClass target)
            throws IOException {
        target.objectId = readId(stream);
    }

    public void readHeapDumpRootThreadBlock(PooledInputStream stream, HprofHeapDumpRootThreadBlock target)
            throws IOException {
        target.objectId = readId(stream);
        target.threadSerial = readU4(stream);
    }

    public void readHeapDumpRootThreadObject(PooledInputStream stream, HprofHeapDumpRootThreadObject target)
            throws IOException {
        target.objectId = readId(stream);
        target.threadSerial = readU4(stream);
        target.stackTraceSerial = readU4(stream);
    }

    public void readHeapDumpRootUnknown(PooledInputStream stream, HprofHeapDumpRootUnknown target)
            throws IOException {
        target.id = readId(stream);
    }
}
