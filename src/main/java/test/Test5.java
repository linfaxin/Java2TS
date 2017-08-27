package test;

/**
 * 测试内部接口
 */
public class Test5 {
    A a = new A() {
        @Override
        public int a() {
            return 0;
        }
    };

    static B b = new B() {
        @Override
        public int b() {
            return 0;
        }
    };

    interface A {
        int a();
    }
    static interface B {
        int b();
    }
    interface C {
        int c();
        interface D {
            int d();
        }
        class E {
            int e = 1;
        }
    }
    interface E {
        C c();
        C.D d();
    }
}
