package io.github.yuanbug.drawer.test.lombok;

import lombok.Setter;

/**
 * @author yuanbug
 */
public class SetterAnnotationOnFieldParse {

    private String id;

    @Setter
    private String name;

    private Integer age;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

}
