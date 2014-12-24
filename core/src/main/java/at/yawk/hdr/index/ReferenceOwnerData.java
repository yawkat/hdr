package at.yawk.hdr.index;

/**
 * Abstract class for data on anything that can hold references to objects (other objects, call stacks)
 *
 * @author yawkat
 */
public abstract class ReferenceOwnerData {
    String name = "";

    public String getName() {
        return name;
    }
}
