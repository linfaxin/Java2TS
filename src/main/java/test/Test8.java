package test;

/**
 * 接口内部类测试
 */
public interface Test8 {
    int b();
    static class AAA {
        int a;
    }
    class BBB {
        int b;
        BBB bbb = new BBB();
        static Test8 test8 = new Test8() {
            @Override
            public int b() {
                new Test8.AAA();
                return 0;
            }
        };
    }
    interface CCC {
        int ccc();
    }
}
