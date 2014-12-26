package at.yawk.hdr.gui;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * @author yawkat
 */
public abstract class Controller {
    private final StringProperty title = new SimpleStringProperty();

    public StringProperty titleProperty() { return title; }

    public String getTitle() {
        return title.get();
    }

    public void setTitle(String title) {
        this.title.set(title);
    }
}
