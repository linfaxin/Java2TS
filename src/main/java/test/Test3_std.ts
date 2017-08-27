namespace test {
    /**
     * 不支持的变量名测试
     */
    export class Test3 {

        static _in: number = 1;

        static in2: number = Test3._in;

        _in(_in: number): number {
            return _in;
        }

        in2(): number {
            let _in: number = 1;
            return _in;
        }
    }
}
