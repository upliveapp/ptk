/*
 * Copyright 2018 xincao9@gmail.com.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.xincao9.ptk.core;

import com.github.xincao9.ptk.core.annotation.scan.AnnotationDetector;
import com.github.xincao9.ptk.core.annotation.scan.AnnotationDetector.TypeReporter;
import com.github.xincao9.ptk.core.annotation.Test;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 扫描器
 * 
 * @author xincao9@gmail.com
 */
public final class MethodScanner {

    private Map<String, Method> methods = new HashMap();
    private static MethodScanner methodService;

    /**
     * 构造器
     */
    private MethodScanner() {
        start();
    }

    /**
     * 获得实例
     * 
     * @return 方法服务
     */
    public static MethodScanner getInstance() {
        if (MethodScanner.methodService == null) {
            MethodScanner.methodService = new MethodScanner();
        }
        return MethodScanner.methodService;
    }
    
    /**
     * 启动方法
     */
    public void start() {
        long begin = System.currentTimeMillis();
        Logger.info("Scanning  annotation, this may take a while, please wait...");
        this.scanForMethods();
        long cost = System.currentTimeMillis() - begin;
        Logger.info(String.format("Finished scan for annotation, found {%s} methods(s), cost={%s} ms", methods.size(), cost));
    }

    /**
     * 扫面所有的Method
     */
    public void scanForMethods() {
        final Map<String, Method> innerMethods = new HashMap();

        /**
         * 内部匿名类
         */
        TypeReporter reporter = new TypeReporter() {
            @Override
            @SuppressWarnings("unchecked")
            public Class<? extends java.lang.annotation.Annotation>[] annotations() {
                return new Class[]{Test.class};
            }

            @Override
            public void reportTypeAnnotation(Class<? extends Annotation> annotationClass, String className) {
                try {
                    Class clazz = Class.forName(className);
                    if (!Method.class.isAssignableFrom(clazz)) {
                        throw new IllegalArgumentException("类 <" + className + "> 没有实现接口 Method");
                    }
                    Test testAnnotation = (Test) clazz.getAnnotation(annotationClass);
                    String name = testAnnotation.name();
                    if (name == null || name.isEmpty()) {
                        throw new IllegalArgumentException("类 <" + className + "> 的 Test Annotation 设置不正确，name 属性必须设置");
                    }
                    Method method = (Method) clazz.newInstance();
                    innerMethods.put(name, method);
                    Logger.info("Found Method, name=" + name + ", Method.class=" + className);
                } catch (ClassNotFoundException e) {
                    Logger.info(String.format("没有找到类 <" + className + ">。error=", e.getMessage()));
                    System.exit(1);
                } catch (Exception e) {
                    Logger.info("Test Annotation 使用不正确，请检查。error=" + e.getMessage());
                    System.exit(1);
                }
            }
        };
        final AnnotationDetector cf = new AnnotationDetector(reporter);
        try {
            cf.detect();
            this.methods = Collections.unmodifiableMap(innerMethods);
        } catch (IOException ex) {
            Logger.info(String.format("Faild to scan methods error={%s}", ex.getMessage()));
            System.exit(1);
        }
    }

    /**
     * 获得方法
     *
     * @param name 方法名
     * @return 方法
     */
    public Method getMethod(String name) {
        if (methods.containsKey(name)) {
            return methods.get(name);
        }
        Logger.info(String.format("method which name = {} is not exist", name));
        return null;
    }

    /**
     * 获得方法名列表
     * 
     * @return 方法名列表
     */
    public List<String> getMethodNames() {
        List<String> names = new ArrayList();
        if (methods == null || methods.isEmpty()) {
            return names;
        }
        methods.entrySet().stream().map((e) -> e.getKey()).forEachOrdered((name) -> {
            names.add(name);
        });
        return names;
    }
}
