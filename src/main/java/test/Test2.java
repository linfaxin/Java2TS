package test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 泛型测试源文件
 */
public class Test2<T> {
    static Map<String, Long> map = new HashMap<String, Long>();
    static Map<String, Long> map2 = new HashMap<>();
    static Map<String, List<String>> map3 = new HashMap<String, List<String>>();
    static Map<String, Map<String, Void>> map4 = new HashMap<>();

    T t;
    List<T> tList;
    Map<T, T> tMap = new HashMap<T, T>();
    Map<String, T> tMap2 = new HashMap<>();

    List<T> getList() {
        return new ArrayList<>();
    }

    List<String> getList2() {
        return new ArrayList<>();
    }

    Map<T, T> getMap1() {
        return new HashMap<T, T>();
    }

    Map<String, T> getMap2() {
        return new HashMap<>();
    }

    T ttt() {
        return null;
    }

    public <P> P ppp(P p) {
        return p;
    }
}
