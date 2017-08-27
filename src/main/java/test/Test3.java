package test;

/**
 * 不支持的变量名测试
 */
public class Test3 {
    static int in = 1;
    static int in2 = Test3.in;

    int in(int in) {
        return in;
    }
    int in2() {
        int in = 1;
        return in;
    }
}
