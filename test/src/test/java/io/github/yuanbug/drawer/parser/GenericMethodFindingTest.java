package io.github.yuanbug.drawer.parser;

import com.github.javaparser.ast.body.MethodDeclaration;
import io.github.yuanbug.drawer.BaseTest;
import io.github.yuanbug.drawer.domain.info.MethodId;
import io.github.yuanbug.drawer.test.generic.GenericMethodFinding;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author yuanbug
 */
public class GenericMethodFindingTest extends BaseTest {


    private void test(String findByTypeName, String expectedParamType) {
        test("justReturn", findByTypeName, expectedParamType);
    }

    private void test(String methodName, String findByTypeName, String expectedParamType) {
        MethodDeclaration result = methodParser.findMethod(new MethodId(
                GenericMethodFinding.class.getName(),
                methodName,
                List.of(findByTypeName)
        )).orElseThrow();
        assertEquals(methodName, result.getNameAsString());
        assertEquals(expectedParamType, result.getParameters().get(0).getTypeAsString());
    }

    @Test
    void testFindByString() {
        test("java.lang.String", "String");
    }

    @Test
    void testFindByT() {
        test("T", "T");
    }

    @Test
    void testFindByObject() {
        test("java.lang.Object", "T");
    }

    @Test
    void testFindByInteger() {
        test("int", "int");
        test("java.lang.Integer", "Integer");
    }

    @Test
    void testFindByLong() {
        test("java.lang.Long", "Long");
        test("long", "Long");
    }

    @Test
    void testFindByDouble() {
        test("java.lang.Double", "Number");
        test("double", "Number");
    }

    @Test
    void testFindByResolvedType() {
        test(GenericMethodFinding.A.class.getName(), "A");
        test(GenericMethodFinding.B.class.getName(), "B");
        test(GenericMethodFinding.I.class.getName(), "I");
        test("returnIt", GenericMethodFinding.A.class.getName(), "I");
        test("returnIt", GenericMethodFinding.B.class.getName(), "I");
    }

    @Test
    void testFindBySomeElse() {
        test("WhatTheFuck", "T");
    }

}
