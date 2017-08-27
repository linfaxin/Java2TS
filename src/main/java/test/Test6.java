package test;

/**
 * 测试内部类 & 指针
 */
public class Test6 {
    A a = new A();
    A1 a1 = new A1();
    static B b = new B();
    static B1 b1 = new B1();
    static int c = b.b + b1.b1;

    public Test6() {
        A a = this.a;
        int d = a.a + a1.a1 + c;
    }

    class A {
        int a = 1;
        A() {
            a = a1.a1;
        }
    }
    private class A1 {
        int a1 = 1;
        A1() {
            a1 = Test6.this.a.a;
        }
    }
    static class B {
        int b = 1;
    }
    private static class B1 {
        int b1 = 1;
    }
}
