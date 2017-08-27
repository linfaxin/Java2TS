namespace test {
    /**
     * 测试内部类 & 指针
     */
    export class Test6 {

        A = ((Test6_this) => class A {

            a: number = 1;

            constructor() {
                a = a1.a1;
            }
        })(this);

        private A1 = ((Test6_this) => class A1 {

            a1: number = 1;

            constructor() {
                a1 = Test6.this.a.a;
            }
        })(this);

        static B = class B {

            b: number = 1;
        }

        private static B1 = class B1 {

            b1: number = 1;
        }

        a: test.Test6.A = new test.Test6.A();

        a1: test.Test6.A1 = new test.Test6.A1();

        static b: test.Test6.B = new test.Test6.B();

        static b1: test.Test6.B1 = new test.Test6.B1();

        static c: number = b.b + b1.b1;

        constructor() {
            let a: test.Test6.A = this.a;
            let d: number = a.a + a1.a1 + c;
        }
    }
}
