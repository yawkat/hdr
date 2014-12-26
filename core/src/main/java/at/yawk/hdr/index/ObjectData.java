package at.yawk.hdr.index;

import gnu.trove.list.TLongList;
import javax.annotation.Nullable;

/**
 * @author yawkat
 */
public class ObjectData {
    public long classId = -1;
    /**
     * If this is an array, the BaseType of the values. If this is a multi-dimensional array, this is BaseType.OBJECT.
     * If this is not an array, -1.
     */
    public byte primitiveArrayType = -1;
    public long id;
    @Nullable public TLongList fields;
}
