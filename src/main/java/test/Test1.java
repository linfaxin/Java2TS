package test;

/**
 * 基本测试
 */
public class Test1 {
    static String s = "aaaa";
    public String a;
    int b = 2;
    protected char c = 'd';
    private float d = 1.1f;

    public void func() {
    }
    private String func2() {
        return "string";
    }
    protected String func3() {
        return "func3";
    }
    String func4() {
        return "func4";
    }
    static String func5() {
        return "func5";
    }
    String func6(String... args) {
        return "func6";
    }
    String func7(String arg1, String... args) {
        return "func7";
    }
}
