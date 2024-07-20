package io.github.yuanbug.drawer.test.lombok;

import lombok.Getter;

/**
 * @author yuanbug
 */
@Getter
public class GetterAnnotationOnTypeParse {

    private String id;

    private String name;

    private Integer number;

    private boolean enable;

    private String UpperCaseField, _underlineField;

    public String getId() {
        return id;
    }

    public String getNameAndNumber() {
        return getName() + ":" + getNumber();
    }

}
