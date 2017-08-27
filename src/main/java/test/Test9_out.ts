namespace test {
    /**
     * 测试 构造器重载 & 方法重载
     */
    export class Test9 {

        constructor() {
            let b: boolean = true;
        }

        constructor(b: boolean) {
            let b2: boolean = b;
        }

        aaa(): void {
            let a: number = 1;
        }

        aaa(s: string): void {
            let a: string = s;
        }

        bbb(): void {
            let b: number = 1;
        }

        bbb(s: number): number {
            return s;
        }

        bbb(s: string): string {
            return s;
        }
    }
}
