namespace test {
    import ArrayList = java.util.ArrayList;
    import HashMap = java.util.HashMap;
    import List = java.util.List;
    import Map = java.util.Map;

    /**
     * 泛型测试源文件
     */
    export class Test2<T> {

        static map: Map<string, number> = new HashMap<string, number>();

        static map2: Map<string, number> = new HashMap();

        static map3: Map<string, List<string>> = new HashMap<string, List<string>>();

        static map4: Map<string, Map<string, void>> = new HashMap();

        t: T;

        tList: List<T>;

        tMap: Map<T, T> = new HashMap<T, T>();

        tMap2: Map<string, T> = new HashMap();

        getList(): List<T> {
            return new ArrayList();
        }

        getList2(): List<string> {
            return new ArrayList();
        }

        getMap1(): Map<T, T> {
            return new HashMap<T, T>();
        }

        getMap2(): Map<string, T> {
            return new HashMap();
        }

        ttt(): T {
            return null;
        }

        public ppp<P>(p: P): P {
            return p;
        }
    }
}
