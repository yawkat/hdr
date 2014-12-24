package at.yawk.hdr.gui.type;

import at.yawk.hdr.index.ReferenceOwnerData;
import lombok.Getter;

/**
 * @author yawkat
 */
@Getter
class ReferenceData {
    private final int referenceCount;
    private final ReferenceOwnerData typeData;

    public ReferenceData(int referenceCount, ReferenceOwnerData typeData) {
        this.referenceCount = referenceCount;
        this.typeData = typeData;
    }
}
