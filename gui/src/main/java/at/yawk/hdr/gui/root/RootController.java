package at.yawk.hdr.gui.root;

import at.yawk.hdr.gui.MainController;
import at.yawk.hdr.index.Indexer;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author yawkat
 */
public interface RootController {
    void init(Indexer indexer, MainController mainController);

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public static @interface Tab {
        String title();

        int priority() default 0;
    }
}
