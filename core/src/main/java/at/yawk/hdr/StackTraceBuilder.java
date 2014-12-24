package at.yawk.hdr;

import at.yawk.hdr.format.HprofStackFrame;
import at.yawk.hdr.format.HprofStackTrace;
import at.yawk.hdr.index.Indexer;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for building stack traces and related thread representations.
 *
 * @author yawkat
 */
public class StackTraceBuilder {
    private final Indexer indexer;

    private final List<StackTraceElement> elements = new ArrayList<>();

    public StackTraceBuilder(Indexer indexer) {
        this.indexer = indexer;
    }

    /**
     * Append a frame by its ID.
     */
    public StackTraceBuilder appendFrame(long frameId) {
        return appendFrame(indexer.getStackFrameIndex().get(frameId));
    }

    public StackTraceBuilder appendFrame(HprofStackFrame frame) {
        elements.add(new StackTraceElement(
                HeapDumpUtil.pathToClassName(
                        indexer.getStringIndex().get(indexer.getClassSerialIndex().get(frame.classSerial).nameId)),
                indexer.getStringIndex().get(frame.methodNameId),
                indexer.getStringIndex().get(frame.sourceFileNameId),
                frame.lineNumber
        ));
        return this;
    }

    /**
     * Append a stack trace by the stack trace serial number.
     */
    public StackTraceBuilder appendTrace(int traceSerial) {
        return appendTrace(indexer.getStackTraceIndex().get(traceSerial));
    }

    public StackTraceBuilder appendTrace(HprofStackTrace trace) {
        for (int i = trace.frameIds.length - 1; i >= 0; i--) {
            long frameId = trace.frameIds[i];
            appendFrame(frameId);
        }
        return this;
    }

    /**
     * Compile this stack trace to a multi-line string.
     */
    public String compile() {
        StringBuilder builder = new StringBuilder();
        if (!elements.isEmpty()) {
            for (StackTraceElement element : elements) {
                builder.append(element).append('\n');
            }
            builder.setLength(builder.length() - 1);
        }
        return builder.toString();
    }

    /**
     * Summarize this stack trace in a single line.
     */
    public String summarize() {
        if (elements.isEmpty()) { return ""; }

        // find topmost element with a non-java-related class name
        for (StackTraceElement element : elements) {
            String className = element.getClassName();

            if (className.startsWith("java.")) { continue; }
            if (className.startsWith("javax.")) { continue; }
            if (className.startsWith("sun.")) { continue; }
            if (className.startsWith("com.sun.")) { continue; }
            if (className.startsWith("com.intellij.")) { continue; }

            return element.toString();
        }
        return elements.get(0).toString();
    }

    @Override
    public String toString() {
        return elements.toString();
    }
}
