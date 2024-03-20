import java.io.File;
import java.io.FileWriter;

/**
 * Created by faxin on 2017/8/27.
 */
public class Main {
    public static void main(String[] args) throws Exception {
        convertClass("android.widget.DatePicker");
        convertClass("android.widget.TimePicker");
    }

    public static void convertClass(String className) throws Exception {
        File javaFile = new File("res/java/" + className.replaceAll("\\.", "/") + ".java");
        String tsSource = Java2TS.convertClassToTSCode(javaFile);

        File outFile = new File("res/output_ts/" + className.replaceAll("\\.", "/") + ".ts");
        outFile.getParentFile().mkdirs();
        FileWriter fw = new FileWriter(outFile);
        fw.write(tsSource);
        fw.close();
    }
}
