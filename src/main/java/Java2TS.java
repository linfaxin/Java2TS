import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import print.TSPrinter;
import print.TSPrinterConfiguration;

import java.io.File;

/**
 * Created by faxin on 2017/8/27.
 */
public class Java2TS {
    public static String convertClassToTSCode(File javaFile, Class c) throws Exception {
        CompilationUnit compilationUnit = JavaParser.parse(javaFile);
        return new TSPrinter(new TSPrinterConfiguration()).print(c, compilationUnit);
    }
}
