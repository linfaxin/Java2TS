import android.widget.DatePicker;
import android.widget.TimePicker;

import java.io.File;
import java.io.FileWriter;

/**
 * Created by faxin on 2017/8/27.
 */
public class Main {
    public static void main(String[] args) throws Exception {
        convertClass(DatePicker.class);
        convertClass(TimePicker.class);
    }

    public static void convertClass(Class c) throws Exception {
        File javaFile = new File("res/java/" + c.getName().replaceAll("\\.", "/") + ".java");
        String tsSource = Java2TS.convertClassToTSCode(javaFile, c);

        File outFile = new File("res/output_ts/" + c.getName().replaceAll("\\.", "/") + ".ts");
        outFile.getParentFile().mkdirs();
        FileWriter fw = new FileWriter(outFile);
        fw.write(tsSource);
        fw.close();
    }
}
