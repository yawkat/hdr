package at.yawk.hdr;

import at.yawk.hdr.format.BaseType;
import java.util.Arrays;

/**
 * @author yawkat
 */
public class HeapDumpUtil {
    private static final String[] primitiveNames = new String['Z' + 1];
    private static final byte[] primitiveBaseTypes = new byte['Z' + 1];

    static {
        primitiveNames['Z'] = "boolean";
        primitiveNames['B'] = "byte";
        primitiveNames['S'] = "short";
        primitiveNames['C'] = "char";
        primitiveNames['I'] = "int";
        primitiveNames['F'] = "float";
        primitiveNames['J'] = "long";
        primitiveNames['D'] = "double";

        Arrays.fill(primitiveBaseTypes, (byte) -1);
        primitiveBaseTypes['Z'] = BaseType.BOOLEAN;
        primitiveBaseTypes['B'] = BaseType.BYTE;
        primitiveBaseTypes['S'] = BaseType.SHORT;
        primitiveBaseTypes['C'] = BaseType.CHAR;
        primitiveBaseTypes['I'] = BaseType.INT;
        primitiveBaseTypes['F'] = BaseType.FLOAT;
        primitiveBaseTypes['J'] = BaseType.LONG;
        primitiveBaseTypes['D'] = BaseType.DOUBLE;
    }

    private HeapDumpUtil() {}

    /**
     * Convert a heap dump class name to a human-readable, java-like one.
     *
     * [B -> byte[]
     * java/lang/String -> java.lang.String
     * [Ljava/lang/String -> java.lang.String[]
     */
    public static String pathToClassName(String pathName) {
        StringBuilder builder = new StringBuilder();
        int i = 0;
        for (; pathName.charAt(i) == '['; i++) { builder.append("[]"); }

        if (i > 0 && i == pathName.length() - 1) {
            builder.insert(0, primitiveNames[pathName.charAt(i)]);
        } else {
            builder.insert(0, pathName, i > 0 ? i + 1 : i, pathName.length() - i);

            for (int j = 0; j < builder.length(); j++) {
                char c = builder.charAt(j);
                if (c == '/' || c == '$') {
                    builder.setCharAt(j, '.');
                }
            }
        }

        return builder.toString();
    }

    /**
     * Return the BaseType of the primitive array type with the given descriptor or -1 if this is not a primitive array
     * descriptor.
     */
    public static byte getPrimitiveArrayType(String descriptor) {
        if (descriptor.length() == 2 && descriptor.charAt(0) == '[') {
            char c = descriptor.charAt(1);
            if (c < primitiveBaseTypes.length) {
                return primitiveBaseTypes[c];
            }
        }
        return -1;
    }
}
