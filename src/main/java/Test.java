import com.oracle.tools.packager.IOUtils;
import util.ClassUtil;

import java.io.File;
import java.io.FileWriter;
import java.util.List;

/**
 * Created by faxin on 2017/8/28.
 * Test entry
 */
public class Test {
    public static void main(String[] args) throws Exception {
        List<Class<?>> classes = ClassUtil.getClasses("test");
        for (Class clas : classes) {
            if (!clas.getName().contains("$")) {
                convertClass(clas);
            }
        }
    }

    public static void convertClass(Class c) throws Exception {
        File javaFile = new File("src/main/java/" + c.getName().replaceAll("\\.", "/") + ".java");
        String tsSource = Java2TS.convertClassToTSCode(javaFile, c);

        File stdOutTsFile = new File("src/main/java/" + c.getName().replaceAll("\\.", "/") + "_std.ts");
        String outFilePath = "src/main/java/" + c.getName().replaceAll("\\.", "/") + "_out.ts";
        if (stdOutTsFile.exists()) {
            String standardOutSource = new String(IOUtils.readFully(stdOutTsFile));
            if (tsSource.equals(standardOutSource)) {
                new File(outFilePath).delete();
                System.out.println(c.getName() + " check ok.");
            } else {
                System.err.println(c.getName() + " not equals the standard output, please check the out file: " + outFilePath);
                writeOutFile(tsSource, c);
            }
        } else {
            System.out.println(c.getName() + " has transformed to file: " + outFilePath + ", please check by eye :)");
            writeOutFile(tsSource, c);
        }
    }

    private static void writeOutFile(String tsSource, Class c) throws Exception {
        File outFile = new File("src/main/java/" + c.getName().replaceAll("\\.", "/") + "_out.ts");
        outFile.getParentFile().mkdirs();
        FileWriter fw = new FileWriter(outFile);
        fw.write(tsSource);
        fw.close();
    }
}
