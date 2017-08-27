namespace test {
    /**
     * 基本测试
     */
    export class Test1 {

        static s: string = "aaaa";

        public a: string;

        b: number = 2;

        protected c: string = 'd';

        private d: number = 1.1;

        public func(): void {
        }

        private func2(): string {
            return "string";
        }

        protected func3(): string {
            return "func3";
        }

        func4(): string {
            return "func4";
        }

        static func5(): string {
            return "func5";
        }

        func6(...args: string[]): string {
            return "func6";
        }

        func7(arg1: string, ...args: string[]): string {
            return "func7";
        }
    }
}
