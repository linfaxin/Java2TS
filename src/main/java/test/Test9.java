package test;

/**
 * 测试 构造器重载 & 方法重载
 */
public class Test9 {
    public Test9() {
        boolean b = true;
    }
    public Test9(boolean b) {
        boolean b2 = b;
    }

    void aaa() {
        int a = 1;
    }

    void aaa(String s) {
        String a = s;
    }

    void bbb() {
        int b = 1;
    }

    int bbb(int s) {
        return s;
    }

    String bbb(String s) {
        return s;
    }
}
