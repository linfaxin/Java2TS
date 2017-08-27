namespace test {
    /**
     * 测试内部静态类的内部接口
     */
    export class Test7 {

        static A1 = class A1 {

            // class or interface 'B' is export in module after root class
        }

        b: test.Test7.A1.B = ((__this) => new class implements test.Test7.A1.B {

            /* @Override */
            public b(): void {
            }
        }())(this);
    }
    export module Test7 {
        export module A1 {
            export interface B {

                b(): void;
            }
        }
    }

}
