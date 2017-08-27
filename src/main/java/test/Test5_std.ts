namespace test {
    /**
     * 测试内部接口
     */
    export class Test5 {

        // class or interface 'A' is export in module after root class

        // class or interface 'B' is export in module after root class

        // class or interface 'C' is export in module after root class

        // class or interface 'E' is export in module after root class

        a: test.Test5.A = ((__this) => new class implements test.Test5.A {

            /* @Override */
            public a(): number {
                return 0;
            }
        }())(this);

        static b: test.Test5.B = new class implements test.Test5.B {

            /* @Override */
            public b(): number {
                return 0;
            }
        }();
    }
    export module Test5 {
        export interface A {

            a(): number;
        }
    }
    export module Test5 {
        export interface B {

            b(): number;
        }
    }
    export module Test5 {
        export interface C {

            // class or interface 'D' is export in module after root class

            // class or interface 'E' is export in module after root class

            c(): number;
        }
    }
    export module Test5 {
        export interface E {

            c(): test.Test5.C;

            d(): test.Test5.C.D;
        }
    }
    export module Test5 {
        export module C {
            export interface D {

                d(): number;
            }
        }
    }
    export module Test5 {
        export module C {
            export class E {

                e: number = 1;
            }
        }
    }

}
