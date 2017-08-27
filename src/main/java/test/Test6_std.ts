namespace test {
    /**
     * 测试内部类 & 指针
     */
    export class Test6 {

        A = ((Test6_this) => class A {

            a: number = 1;

            constructor() {
                this.a = Test6_this.a1.a1;
            }
        })(this);

        private A1 = ((Test6_this) => class A1 {

            a1: number = 1;

            constructor() {
                this.a1 = Test6_this.a.a;
            }
        })(this);

        static B = class B {

            b: number = 1;
        }

        private static B1 = class B1 {

            b1: number = 1;
        }

        a = new this.A();

        a1 = new this.A1();

        static b = new Test6.B();

        static b1 = new Test6.B1();

        static c: number = Test6.b.b + Test6.b1.b1;

        constructor() {
            let a = this.a;
            let d: number = a.a + this.a1.a1 + Test6.c;
        }
    }
}
