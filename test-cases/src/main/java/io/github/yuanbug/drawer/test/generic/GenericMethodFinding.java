package io.github.yuanbug.drawer.test.generic;

/**
 * @author yuanbug
 */
public class GenericMethodFinding {

    public interface I {}

    public static class A implements I {}

    public static class B extends A {}

    public static abstract class C extends B {}

    public static class D extends C {}

    public <T> T justReturn(T data) {
        return data;
    }

    public String justReturn(String data) {
        return data;
    }

    public Number justReturn(Number data) {
        return data;
    }

    public Long justReturn(Long data) {
        return data;
    }

    public Integer justReturn(Integer data) {
        return data;
    }

    public int justReturn(int data) {
        return data;
    }

    public CharSequence justReturn(CharSequence data) {
        return data;
    }

    public A justReturn(A data) {
        return data;
    }

    public B justReturn(B data) {
        return data;
    }

    public I justReturn(I data) {
        return data;
    }

    public I returnIt(I data) {
        return data;
    }

}
