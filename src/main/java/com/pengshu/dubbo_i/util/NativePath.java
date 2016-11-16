package com.pengshu.dubbo_i.util;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class NativePath {
    private static final Logger LOGGER = LogManager.getLogger(NativePath.class.getName());

    public NativePath() {
    }

    public static Path get(String path) {
        if(path.startsWith("/")) {
            path = path.substring(1);
        }

        String java_class_path = get_class_path();
        if(java_class_path.endsWith(".jar")) {
            int lastIndexOf = java_class_path.lastIndexOf("/");
            if(lastIndexOf == -1) {
                java_class_path = "";
            } else {
                java_class_path = java_class_path.substring(0, lastIndexOf);
            }
        }

        if(!java_class_path.isEmpty() && !java_class_path.endsWith("/")) {
            java_class_path = java_class_path.concat("/");
        }

        java_class_path = java_class_path.concat(path);
        LOGGER.info("final path ---> :".concat(java_class_path));
        return Paths.get(java_class_path, new String[0]);
    }

    public static String get_class_path(Class<?> clazz) {
        String location = clazz.getProtectionDomain().getCodeSource().getLocation().getFile();
        location = location.replace("file:", "");
        if(System.getProperty("os.name").indexOf("Windows") != -1) {
            location = location.substring(1);
        }

        if(location.contains(".jar!")) {
            location = location.substring(0, location.indexOf(".jar!")).concat(".jar");
        }

        if(location.endsWith("/")) {
            location = location.substring(0, location.length() - 1);
        }

        return location;
    }

    public static String get_class_path() {
        String java_class_path = System.getProperty("java.class.path");
        LOGGER.debug("java_class_path -> :".concat(java_class_path));
        LOGGER.debug(System.getProperty("os.name"));
        int indexof_classes;
        int webroot;
        int indexof_web_inf;
        int comma;
        String webroot1;
        if(System.getProperty("os.name").indexOf("Windows") != -1) {
            indexof_classes = java_class_path.indexOf("\\classes");
            if(indexof_classes != -1) {
                java_class_path = java_class_path.substring(0, indexof_classes).concat("\\classes");
                webroot = java_class_path.lastIndexOf(";");
                if(webroot != -1) {
                    java_class_path = java_class_path.substring(webroot + 1);
                }

                LOGGER.debug("windows code start --> :".concat(java_class_path));
            } else {
                webroot1 = NativePath.class.getResource("").getFile();
                webroot1 = webroot1.replace("file:/", "");
                indexof_web_inf = webroot1.indexOf("/WEB-INF/");
                if(indexof_web_inf != -1) {
                    java_class_path = webroot1.substring(0, indexof_web_inf).concat("/WEB-INF/classes");
                    LOGGER.debug("windows server start --> :".concat(java_class_path));
                } else {
                    comma = java_class_path.indexOf(";");
                    if(comma > 0) {
                        java_class_path = java_class_path.substring(0, comma);
                    }

                    LOGGER.debug("windows jar start --> :".concat(java_class_path));
                }
            }
        } else {
            indexof_classes = java_class_path.indexOf("/classes");
            if(indexof_classes != -1) {
                java_class_path = java_class_path.substring(0, indexof_classes).concat("/classes");
                webroot = java_class_path.lastIndexOf(":");
                if(webroot != -1) {
                    java_class_path = java_class_path.substring(webroot + 1);
                }

                LOGGER.debug("linux code start --> :".concat(java_class_path));
            } else {
                webroot1 = NativePath.class.getResource("").getFile();
                webroot1 = webroot1.replace("file:", "");
                indexof_web_inf = webroot1.indexOf("/WEB-INF/");
                if(indexof_web_inf != -1) {
                    java_class_path = webroot1.substring(0, indexof_web_inf).concat("/WEB-INF/classes");
                    LOGGER.debug("linux server start --> :".concat(java_class_path));
                } else {
                    comma = java_class_path.indexOf(":");
                    if(comma > 0) {
                        java_class_path = java_class_path.substring(0, comma);
                    }

                    LOGGER.debug("linux jar start --> :".concat(java_class_path));
                }
            }
        }

        return java_class_path;
    }
}
