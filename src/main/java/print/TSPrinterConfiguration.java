package print;/*
 * Copyright (C) 2007-2010 Júlio Vilmar Gesser.
 * Copyright (C) 2011, 2013-2016 The JavaParser Team.
 *
 * This file is part of JavaParser.
 *
 * JavaParser can be used either under the terms of
 * a) the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * b) the terms of the Apache License
 *
 * You should have received a copy of both licenses in LICENCE.LGPL and
 * LICENCE.APACHE. Please refer to those files for details.
 *
 * JavaParser is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 */

import java.util.function.Function;

import static com.github.javaparser.utils.Utils.EOL;
import static com.github.javaparser.utils.Utils.assertNotNull;

public class TSPrinterConfiguration {
    private boolean printComments = true;
    private boolean printJavaDoc = true;
    private String indent = "    ";
    private String notSupportText = "java2ts not support";
    private String endOfLineCharacter = EOL;
    private boolean ignoreNotSupportException = true;

    public String getIndent() {
        return indent;
    }

    public TSPrinterConfiguration setIndent(String indent) {
        this.indent = assertNotNull(indent);
        return this;
    }

    public boolean isPrintComments() {
        return printComments;
    }

    public boolean isIgnoreComments() {
        return !printComments;
    }

    public boolean isPrintJavaDoc() {
        return printJavaDoc;
    }

    public TSPrinterConfiguration setPrintComments(boolean printComments) {
        this.printComments = printComments;
        return this;
    }

    public TSPrinterConfiguration setPrintJavaDoc(boolean printJavaDoc) {
        this.printJavaDoc = printJavaDoc;
        return this;
    }

    public String getEndOfLineCharacter() {
        return endOfLineCharacter;
    }

    public TSPrinterConfiguration setEndOfLineCharacter(String endOfLineCharacter) {
        this.endOfLineCharacter = assertNotNull(endOfLineCharacter);
        return this;
    }

    public boolean isIgnoreNotSupportException() {
        return ignoreNotSupportException;
    }

    public void setIgnoreNotSupportException(boolean ignoreNotSupportException) {
        this.ignoreNotSupportException = ignoreNotSupportException;
    }

    public String getNotSupportText() {
        return notSupportText;
    }

    public void setNotSupportText(String notSupportText) {
        this.notSupportText = notSupportText;
    }
}
