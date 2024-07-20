package io.github.yuanbug.drawer.test.lombok;

import lombok.Data;

/**
 * @author yuanbug
 */
@Data
public class DataAnnotationOnTypeParse {

    private String id;

    private String name;

    private Integer age;

    public String getId() {
        return id;
    }

    public void setName(String name) {
        this.name = name;
    }

}
