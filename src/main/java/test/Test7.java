package test;

/**
 * 测试内部静态类的内部接口
 */
public class Test7 {
    A1.B b = new A1.B() {
        @Override
        public void b() {
        }
    };
    static class A1 {
        interface B {
            void b();
        }
    }
}
