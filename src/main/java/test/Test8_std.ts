namespace test {
    /**
     * 接口内部类测试
     */
    export interface Test8 {

        // class or interface 'AAA' is export in module after root class

        // class or interface 'BBB' is export in module after root class

        // class or interface 'CCC' is export in module after root class

        b(): number;
    }
    export module Test8 {
        export class AAA {

            a: number;
        }
    }
    export module Test8 {
        export class BBB {

            b: number;

            bbb: test.Test8.BBB = new test.Test8.BBB();

            static test8: test.Test8 = new class implements test.Test8 {

                /* @Override */
                public b(): number {
                    new test.Test8.AAA();
                    return 0;
                }
            }();
        }
    }
    export module Test8 {
        export interface CCC {

            ccc(): number;
        }
    }

}
