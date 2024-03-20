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

import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.comments.BlockComment;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.modules.*;
import com.github.javaparser.ast.nodeTypes.NodeWithTypeArguments;
import com.github.javaparser.ast.nodeTypes.NodeWithVariables;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.type.*;
import com.github.javaparser.ast.visitor.Visitable;
import com.github.javaparser.ast.visitor.VoidVisitor;

import java.util.*;
import java.util.stream.Collectors;

import static com.github.javaparser.ast.Node.Parsedness.UNPARSABLE;
import static com.github.javaparser.utils.PositionUtils.sortByBeginPosition;
import static com.github.javaparser.utils.Utils.isNullOrEmpty;

/**
 * Outputs the AST as formatted Java source code.
 *
 * @author Julio Vilmar Gesser
 */
public class TSPrintVisitor implements VoidVisitor<Void> {
    protected final TSPrinterConfiguration configuration;
    protected final SourcePrinter printer;
    private CompilationUnit rootNode;
    private ArrayList<ClassOrInterfaceDeclaration> tsModules = new ArrayList<>();

    public TSPrintVisitor(TSPrinterConfiguration prettyPrinterConfiguration, CompilationUnit rootNode) {
        configuration = prettyPrinterConfiguration;
        printer = new SourcePrinter(configuration.getIndent(), configuration.getEndOfLineCharacter());
        this.rootNode = rootNode;
    }

    public String getSource() {
        return printer.getSource();
    }

    private void warnNotSupport(Node notSupportNode, String notSupportReason) {
        printer.println("/* " + configuration.getNotSupportText() + " : " + notSupportReason + " */");
        if (notSupportNode != null) printer.println("/* " + notSupportNode.toString() + " */");
        if (configuration.isIgnoreNotSupportException()) {
            System.out.print("[WARN] " + notSupportReason + " :::: " + (notSupportNode != null ? notSupportNode.toString() : ""));
        } else {
            throw new RuntimeException(notSupportReason);
        }
    }

    private void printModifiers(final EnumSet<Modifier> modifiers) {
        if (modifiers.size() > 0) {
            // all TypeScript support modifier
            ArrayList<String> supportModifiers = new ArrayList<>();
            ArrayList<Modifier> allModifiers = new ArrayList<>(modifiers);
            if (allModifiers.remove(Modifier.PRIVATE)) {
                supportModifiers.add(Modifier.PRIVATE.asString());
            }
            if (allModifiers.remove(Modifier.PROTECTED)) {
                supportModifiers.add(Modifier.PROTECTED.asString());
            }
            if (allModifiers.remove(Modifier.PUBLIC)) {
                supportModifiers.add(Modifier.PUBLIC.asString());
            }
            if (allModifiers.remove(Modifier.ABSTRACT)) {
                supportModifiers.add(Modifier.ABSTRACT.asString());
            }
            if (allModifiers.remove(Modifier.STATIC)) {
                supportModifiers.add(Modifier.STATIC.asString());
            }
            // let or const
            allModifiers.remove(Modifier.FINAL); // final check not here
            if (allModifiers.size() > 0) {
                warnNotSupport(null, "not support modifiers: " + allModifiers.stream().map(Modifier::asString).collect(Collectors.joining(" ")) + " ");
            }
            printer.print(supportModifiers.stream().collect(Collectors.joining(" ")) + " ");
        }
    }

    private void printMembers(final List<BodyDeclaration<?>> members, final Void arg) {
        for (final BodyDeclaration<?> member : members) {
            printer.println();
            member.accept(this, arg);
            printer.println();
        }
    }

    private void printMemberAnnotations(final NodeList<AnnotationExpr> annotations, final Void arg) {
        if (annotations.isEmpty()) {
            return;
        }
        for (final AnnotationExpr a : annotations) {
            a.accept(this, arg);
            printer.println();
        }
    }

    private void printAnnotations(final NodeList<AnnotationExpr> annotations, boolean prefixWithASpace,
                                  final Void arg) {
        if (annotations.isEmpty()) {
            return;
        }
        if (prefixWithASpace) {
            printer.print(" ");
        }
        for (AnnotationExpr annotation : annotations) {
            annotation.accept(this, arg);
            printer.print(" ");
        }
    }

    private void printTypeArgs(final NodeWithTypeArguments<?> nodeWithTypeArguments, final Void arg) {
        NodeList<Type> typeArguments = nodeWithTypeArguments.getTypeArguments().orElse(null);
        if (!isNullOrEmpty(typeArguments)) {
            printer.print("<");
            for (final Iterator<Type> i = typeArguments.iterator(); i.hasNext(); ) {
                final Type t = i.next();
                t.accept(this, arg);
                if (i.hasNext()) {
                    printer.print(", ");
                }
            }
            printer.print(">");
        }
    }

    private void printTypeParameters(final NodeList<TypeParameter> args, final Void arg) {
        if (!isNullOrEmpty(args)) {
            printer.print("<");
            for (final Iterator<TypeParameter> i = args.iterator(); i.hasNext(); ) {
                final TypeParameter t = i.next();
                t.accept(this, arg);
                if (i.hasNext()) {
                    printer.print(", ");
                }
            }
            printer.print(">");
        }
    }

    private void printArguments(final NodeList<Expression> args, final Void arg) {
        printer.print("(");
        if (!isNullOrEmpty(args)) {
            for (final Iterator<Expression> i = args.iterator(); i.hasNext(); ) {
                final Expression e = i.next();
                e.accept(this, arg);
                if (i.hasNext()) {
                    printer.print(", ");
                }
            }
        }
        printer.print(")");
    }

    private void printPrePostFixOptionalList(final NodeList<? extends Visitable> args, final Void arg, String prefix, String separator, String postfix) {
        if (!args.isEmpty()) {
            printer.print(prefix);
            for (final Iterator<? extends Visitable> i = args.iterator(); i.hasNext(); ) {
                final Visitable v = i.next();
                v.accept(this, arg);
                if (i.hasNext()) {
                    printer.print(separator);
                }
            }
            printer.print(postfix);
        }
    }

    private void printPrePostFixRequiredList(final NodeList<? extends Visitable> args, final Void arg, String prefix, String separator, String postfix) {
        printer.print(prefix);
        if (!args.isEmpty()) {
            for (final Iterator<? extends Visitable> i = args.iterator(); i.hasNext(); ) {
                final Visitable v = i.next();
                v.accept(this, arg);
                if (i.hasNext()) {
                    printer.print(separator);
                }
            }
        }
        printer.print(postfix);
    }

    private void printJavaComment(final Optional<Comment> javacomment, final Void arg) {
        if (configuration.isPrintJavaDoc()) {
            javacomment.ifPresent(c -> c.accept(this, arg));
        }
    }

    @Override
    public void visit(final CompilationUnit n, final Void arg) {
        printJavaComment(n.getComment(), arg);
        if (n.getParsed() == UNPARSABLE) {
            printer.println("???");
            return;
        }

        if (n.getPackageDeclaration().isPresent()) {
            n.getPackageDeclaration().get().accept(this, arg);
        }
//        printer.indent();

        n.getImports().accept(this, arg);
        if (!n.getImports().isEmpty()) {
            printer.println();
        }

        for (final Iterator<TypeDeclaration<?>> i = n.getTypes().iterator(); i.hasNext(); ) {
            TypeDeclaration<?> typeDeclaration = i.next();
            if (typeDeclaration instanceof ClassOrInterfaceDeclaration) {
                printRootClassOrInterfaceContent((ClassOrInterfaceDeclaration) typeDeclaration, arg);
            } else {
                typeDeclaration.accept(this, arg);
            }
            printer.println();
            if (i.hasNext()) {
                printer.println();
            }
        }
//        printer.unindent();
//        printer.println("}");

        n.getModule().ifPresent(m -> m.accept(this, arg));

        printOrphanCommentsEnding(n);
    }

    @Override
    public void visit(final PackageDeclaration n, final Void arg) {
        printJavaComment(n.getComment(), arg);
        printAnnotations(n.getAnnotations(), false, arg);
//        printer.print("namespace ");
//        n.getName().accept(this, arg);
//        printer.println(" {");

//        printOrphanCommentsEnding(n);
    }

    @Override
    public void visit(final NameExpr n, final Void arg) {
        printJavaComment(n.getComment(), arg);
        n.getName().accept(this, arg);

        printOrphanCommentsEnding(n);
    }

    @Override
    public void visit(final Name n, final Void arg) {
        printJavaComment(n.getComment(), arg);
        if (n.getQualifier().isPresent()) {
            n.getQualifier().get().accept(this, arg);
            printer.print(".");
        }
        printAnnotations(n.getAnnotations(), false, arg);
        printer.print(n.getIdentifier());

        printOrphanCommentsEnding(n);
    }

    @Override
    public void visit(SimpleName n, Void arg) {
        switch (n.getIdentifier()) {
            case "in":
                printer.print("_" + n.getIdentifier());
                break;
            default:
                printer.print(n.getIdentifier());
        }
    }

    @Override
    public void visit(final ClassOrInterfaceDeclaration n, final Void arg) {
        if (n.isInterface() || deepFindParentIsInterface(n)) {
            tsModules.add(n); // will be exported module
            printer.print("// class or interface '");
            n.getName().accept(this, arg);
            printer.print("' is export in module after root class");
            return;
        }
        // only nested type class goto here

        printJavaComment(n.getComment(), arg);
        printMemberAnnotations(n.getAnnotations(), arg);

        printModifiers(n.getModifiers());
        n.getName().accept(this, arg);
        printer.print(" = ");
        if (n.getModifiers().contains(Modifier.STATIC)) {
            printClassOrInterfaceContent(n, arg);
        } else {
            printer.print("((" + ((TypeDeclaration)n.getParentNode().get()).getName().getIdentifier() + "_this) => ");
            printClassOrInterfaceContent(n, arg);
            printer.print(")(this);");
        }
    }

    private boolean deepFindParentIsInterface(ClassOrInterfaceDeclaration n) {
        Optional<Node> parent = n.getParentNode();
        if (parent.isPresent() && parent.get() instanceof ClassOrInterfaceDeclaration) {
            ClassOrInterfaceDeclaration parentClass = (ClassOrInterfaceDeclaration) parent.get();
            if (parentClass.isInterface()) {
                return true;
            } else {
                return deepFindParentIsInterface(parentClass);
            }
        } else {
            return false;
        }
    }

    private void printRootClassOrInterfaceContent(final ClassOrInterfaceDeclaration n, final Void arg) {
        printJavaComment(n.getComment(), arg);
        printMemberAnnotations(n.getAnnotations(), arg);
        if (n.getModifiers().contains(Modifier.PUBLIC)) {
            printer.print("export ");
        }
        printClassOrInterfaceContent(n, arg);
        if (tsModules.size() > 0) printer.println();

        ArrayList<ClassOrInterfaceDeclaration> findModules = new ArrayList<>(tsModules);
        tsModules.clear();
        while (findModules.size() > 0) {
            for (ClassOrInterfaceDeclaration c : findModules) {
                printClassOrInterfaceAsModule(c, arg);
            }
            findModules = new ArrayList<>(tsModules);
            tsModules.clear();
        }
    }
    private void printClassOrInterfaceAsModule(ClassOrInterfaceDeclaration n, final Void arg) {
        ArrayList<String> moduleNames = new ArrayList<>();
        ClassOrInterfaceDeclaration parent = (ClassOrInterfaceDeclaration) n.getParentNode().orElse(null);
        while (parent != null) {
            moduleNames.add(0, parent.getNameAsString());
            Node parentNode = parent.getParentNode().orElse(null);
            if (parentNode instanceof ClassOrInterfaceDeclaration) {
                parent = (ClassOrInterfaceDeclaration) parentNode;
            } else {
                break;
            }
        }
        for (String module : moduleNames) {
            printer.println("export module " + module + " {");
            printer.indent();
        }
        printJavaComment(n.getComment(), arg);
        printMemberAnnotations(n.getAnnotations(), arg);
        printer.print("export ");
        printClassOrInterfaceContent(n, arg);
        printer.println();
        for (String ignored : moduleNames) {
            printer.unindent();
            printer.println("}");
        }
    }
    private void printClassOrInterfaceContent(final ClassOrInterfaceDeclaration n, final Void arg) {
        if (n.isInterface()) {
            printer.print("interface ");
        } else {
            if (n.getModifiers().contains(Modifier.ABSTRACT)) {
                printer.print("abstract ");
            }
            printer.print("class ");
        }
        n.getName().accept(this, arg);
        printTypeParameters(n.getTypeParameters(), arg);

        if (!n.getExtendedTypes().isEmpty()) {
            printer.print(" extends ");
            for (final Iterator<ClassOrInterfaceType> i = n.getExtendedTypes().iterator(); i.hasNext(); ) {
                final ClassOrInterfaceType c = i.next();
                c.accept(this, arg);
                if (i.hasNext()) {
                    printer.print(", ");
                }
            }
        }

        if (!n.getImplementedTypes().isEmpty()) {
            printer.print(" implements ");
            for (final Iterator<ClassOrInterfaceType> i = n.getImplementedTypes().iterator(); i.hasNext(); ) {
                final ClassOrInterfaceType c = i.next();
                c.accept(this, arg);
                if (i.hasNext()) {
                    printer.print(", ");
                }
            }
        }

        printer.println(" {");
        printer.indent();
        if (!isNullOrEmpty(n.getMembers())) {
            // print static class first
            List<BodyDeclaration<?>> classBodyList = new ArrayList<>();
            List<BodyDeclaration<?>> otherBodyList = new ArrayList<>();
            for (final BodyDeclaration<?> member : n.getMembers()) {
                if (member instanceof ClassOrInterfaceDeclaration) {
                    classBodyList.add(member);
                } else {
                    otherBodyList.add(member);
                }
            }
            printMembers(classBodyList, arg);
            printMembers(otherBodyList, arg);
        }

        printOrphanCommentsEnding(n);

        printer.unindent();
        printer.print("}");
    }

    @Override
    public void visit(final JavadocComment n, final Void arg) {
        printer.print("/**");
        String[] lines = n.getContent().split("\n");
        for (int i = 0, length = lines.length; i < length; i++ ) {
            if (i < length - 1) {
                if (lines[i].trim().length() > 0) {
                    printer.println(" " + lines[i].trim());
                } else {
                    printer.println();
                }
            } else {
                printer.print(" " + lines[i].trim());
            }
        }
//        printer.print(n.getContent());
        printer.println("*/");
    }

    @Override
    public void visit(final ClassOrInterfaceType n, final Void arg) {
        printJavaComment(n.getComment(), arg);
        if (n.getScope().isPresent()) {
            n.getScope().get().accept(this, arg);
            printer.print(".");
        }
        for (AnnotationExpr ae : n.getAnnotations()) {
            ae.accept(this, arg);
            printer.print(" ");
        }

        String className = n.getName().asString();
        if(className.equals("Object")) printer.print("any");
        else if(className.equals("String")) printer.print("string");
        else if(className.equals("CharSequence")) printer.print("string");
        else if(className.equals("Number")) printer.print("number");
        else if(className.equals("Float")) printer.print("number");
        else if(className.equals("Integer")) printer.print("number");
        else if(className.equals("Double")) printer.print("number");
        else if(className.equals("Long")) printer.print("number");
        else if(className.equals("Void")) printer.print("void");
        else {
            if (n.getScope().isPresent()) {
                printer.print(className);
            } else {
                try {
                    Class c = ClassFinder.findClass(n);
                    printer.print(c.getName().replaceAll("\\$", "."));
                } catch (Exception ignore) {
                    printer.print(className);
                }
            }

            if (n.isUsingDiamondOperator()) {
//            printer.print("<>");
                // no need print diamond operator
            } else {
                printTypeArgs(n, arg);
            }
        }
    }

    @Override
    public void visit(final TypeParameter n, final Void arg) {
        printJavaComment(n.getComment(), arg);
        for (AnnotationExpr ann : n.getAnnotations()) {
            ann.accept(this, arg);
            printer.print(" ");
        }
        n.getName().accept(this, arg);
        if (!isNullOrEmpty(n.getTypeBound())) {
            printer.print(" extends ");
            for (final Iterator<ClassOrInterfaceType> i = n.getTypeBound().iterator(); i.hasNext(); ) {
                final ClassOrInterfaceType c = i.next();
                c.accept(this, arg);
                if (i.hasNext()) {
                    printer.print(" & ");
                }
            }
        }
    }

    @Override
    public void visit(final PrimitiveType n, final Void arg) {
        printJavaComment(n.getComment(), arg);
        printAnnotations(n.getAnnotations(), true, arg);
//        printer.print(n.getType().asString());
        if (n.getType() == PrimitiveType.Primitive.BOOLEAN) printer.print("boolean");
        if (n.getType() == PrimitiveType.Primitive.CHAR) printer.print("string");
        if (n.getType() == PrimitiveType.Primitive.BYTE) printer.print("number");
        if (n.getType() == PrimitiveType.Primitive.SHORT) printer.print("number");
        if (n.getType() == PrimitiveType.Primitive.INT) printer.print("number");
        if (n.getType() == PrimitiveType.Primitive.LONG) printer.print("number");
        if (n.getType() == PrimitiveType.Primitive.FLOAT) printer.print("number");
        if (n.getType() == PrimitiveType.Primitive.DOUBLE) printer.print("number");
    }

    @Override
    public void visit(final ArrayType n, final Void arg) {
        final List<ArrayType> arrayTypeBuffer = new LinkedList<>();
        Type type = n;
        while (type instanceof ArrayType) {
            final ArrayType arrayType = (ArrayType) type;
            arrayTypeBuffer.add(arrayType);
            type = arrayType.getComponentType();
        }

        type.accept(this, arg);
        for (ArrayType arrayType : arrayTypeBuffer) {
            printAnnotations(arrayType.getAnnotations(), true, arg);
            printer.print("[]");
        }
    }

    @Override
    public void visit(final ArrayCreationLevel n, final Void arg) {
        printAnnotations(n.getAnnotations(), true, arg);
        printer.print("[");
        if (n.getDimension().isPresent()) {
            n.getDimension().get().accept(this, arg);
        }
        printer.print("]");
    }

    @Override
    public void visit(final IntersectionType n, final Void arg) {
        printJavaComment(n.getComment(), arg);
        printAnnotations(n.getAnnotations(), false, arg);
        boolean isFirst = true;
        for (ReferenceType element : n.getElements()) {
            if (isFirst) {
                isFirst = false;
            } else {
                printer.print(" & ");
            }
            element.accept(this, arg);
        }
    }

    @Override
    public void visit(final UnionType n, final Void arg) {
        printJavaComment(n.getComment(), arg);
        printAnnotations(n.getAnnotations(), true, arg);
        boolean isFirst = true;
        for (ReferenceType element : n.getElements()) {
            if (isFirst) {
                isFirst = false;
            } else {
                printer.print(" | ");
            }
            element.accept(this, arg);
        }
    }

    @Override
    public void visit(final WildcardType n, final Void arg) {
        printJavaComment(n.getComment(), arg);
        printAnnotations(n.getAnnotations(), false, arg);
        printer.print("?");
        if (n.getExtendedType().isPresent()) {
            printer.print(" extends ");
            n.getExtendedType().get().accept(this, arg);
        }
        if (n.getSuperType().isPresent()) {
            printer.print(" super ");
            n.getSuperType().get().accept(this, arg);
        }
    }

    @Override
    public void visit(final UnknownType n, final Void arg) {
        // Nothing to print
    }

    @Override
    public void visit(final FieldDeclaration n, final Void arg) {
        printOrphanCommentsBeforeThisChildNode(n);

        printJavaComment(n.getComment(), arg);
        printMemberAnnotations(n.getAnnotations(), arg);
        if (n.getParentNode().get() instanceof ClassOrInterfaceDeclaration
                && ((ClassOrInterfaceDeclaration)n.getParentNode().get()).isInterface()) {
            // don't print modifiers for interface field
        } else {
            printModifiers(n.getModifiers());
        }

        for (final Iterator<VariableDeclarator> i = n.getVariables().iterator(); i.hasNext(); ) {
            final VariableDeclarator var = i.next();
            var.accept(this, arg);
            if (i.hasNext()) {
                printer.print(", ");
            }
        }
//        printer.print(": ");
//        if (!n.getVariables().isEmpty()) {
//            n.getMaximumCommonType().accept(this, arg);
//        }

        printer.print(";");
    }

    @Override
    public void visit(final VariableDeclarator n, final Void arg) {
        printJavaComment(n.getComment(), arg);
        n.getName().accept(this, arg);
        printer.print(": ");
        n.getType().accept(this, arg);

        Optional<NodeWithVariables> ancestor = n.getAncestorOfType(NodeWithVariables.class);
        if (!ancestor.isPresent()) {
            throw new RuntimeException("Unable to work with VariableDeclarator not owned by a NodeWithVariables");
        }
        Type commonType = ancestor.get().getMaximumCommonType();

        Type type = n.getType();

        ArrayType arrayType = null;

        for (int i = commonType.getArrayLevel(); i < type.getArrayLevel(); i++) {
            if (arrayType == null) {
                arrayType = (ArrayType) type;
            } else {
                arrayType = (ArrayType) arrayType.getComponentType();
            }
            printAnnotations(arrayType.getAnnotations(), true, arg);
            printer.print("[]");
        }

        if (n.getInitializer().isPresent()) {
            printer.print(" = ");
            n.getInitializer().get().accept(this, arg);
        }
    }

    @Override
    public void visit(final ArrayInitializerExpr n, final Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print("{");
        if (!isNullOrEmpty(n.getValues())) {
            printer.print(" ");
            for (final Iterator<Expression> i = n.getValues().iterator(); i.hasNext(); ) {
                final Expression expr = i.next();
                expr.accept(this, arg);
                if (i.hasNext()) {
                    printer.print(", ");
                }
            }
            printer.print(" ");
        }
        printer.print("}");
    }

    @Override
    public void visit(final VoidType n, final Void arg) {
        printJavaComment(n.getComment(), arg);
        printAnnotations(n.getAnnotations(), false, arg);
        printer.print("void");
    }

    @Override
    public void visit(final ArrayAccessExpr n, final Void arg) {
        printJavaComment(n.getComment(), arg);
        n.getName().accept(this, arg);
        printer.print("[");
        n.getIndex().accept(this, arg);
        printer.print("]");
    }

    @Override
    public void visit(final ArrayCreationExpr n, final Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print("new ");
        n.getElementType().accept(this, arg);
        for (ArrayCreationLevel level : n.getLevels()) {
            level.accept(this, arg);
        }
        if (n.getInitializer().isPresent()) {
            printer.print(" ");
            n.getInitializer().get().accept(this, arg);
        }
    }

    @Override
    public void visit(final AssignExpr n, final Void arg) {
        printJavaComment(n.getComment(), arg);
        n.getTarget().accept(this, arg);
        printer.print(" ");
        printer.print(n.getOperator().asString());
        printer.print(" ");
        n.getValue().accept(this, arg);
    }

    @Override
    public void visit(final BinaryExpr n, final Void arg) {
        printJavaComment(n.getComment(), arg);
        n.getLeft().accept(this, arg);
        printer.print(" ");
        if (n.getOperator() == BinaryExpr.Operator.EQUALS) {
            printer.print("===");
        } else if (n.getOperator() == BinaryExpr.Operator.NOT_EQUALS) {
            printer.print("!==");
        } else {
            printer.print(n.getOperator().asString());
        }
        printer.print(" ");
        n.getRight().accept(this, arg);
    }

    @Override
    public void visit(final CastExpr n, final Void arg) {
        printJavaComment(n.getComment(), arg);
        String javaType = n.getType().toString();
        if(javaType.equals("int") || javaType.equals("long")){ // default out is int
            printer.print("Math.floor(");
            n.getExpression().accept(this, arg);
            printer.print(")");
        }else {
            printer.print("<");
            n.getType().accept(this, arg);
            printer.print(">");
            n.getExpression().accept(this, arg);
        }
    }

    @Override
    public void visit(final ClassExpr n, final Void arg) {
        printJavaComment(n.getComment(), arg);
        n.getType().accept(this, arg);
        printer.print(".class");
    }

    @Override
    public void visit(final ConditionalExpr n, final Void arg) {
        printJavaComment(n.getComment(), arg);
        n.getCondition().accept(this, arg);
        printer.print(" ? ");
        n.getThenExpr().accept(this, arg);
        printer.print(" : ");
        n.getElseExpr().accept(this, arg);
    }

    @Override
    public void visit(final EnclosedExpr n, final Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print("(");
        n.getInner().accept(this, arg);
        printer.print(")");
    }

    @Override
    public void visit(final FieldAccessExpr n, final Void arg) {
        printJavaComment(n.getComment(), arg);
        n.getScope().accept(this, arg);
        printer.print(".");
        n.getName().accept(this, arg);
    }

    @Override
    public void visit(final InstanceOfExpr n, final Void arg) {
        printJavaComment(n.getComment(), arg);
        n.getExpression().accept(this, arg);
        printer.print(" instanceof ");
        n.getType().accept(this, arg);
    }

    @Override
    public void visit(final CharLiteralExpr n, final Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print("'");
        printer.print(n.getValue());
        printer.print("'");
    }

    @Override
    public void visit(final DoubleLiteralExpr n, final Void arg) {
        printJavaComment(n.getComment(), arg);
        String value = n.getValue();
        if(value.endsWith("f") || value.endsWith("F") || value.endsWith("d") || value.endsWith("D")) {
            value = value.substring(0, value.length()-1);
        }
        printer.print(value);
    }

    @Override
    public void visit(final IntegerLiteralExpr n, final Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print(n.getValue());
    }

    @Override
    public void visit(final LongLiteralExpr n, final Void arg) {
        printJavaComment(n.getComment(), arg);
        String value = n.getValue();
        if(value.endsWith("l") || value.endsWith("L")) {
            value = value.substring(0, value.length()-1);
        }
        printer.print(value);
    }

    @Override
    public void visit(final StringLiteralExpr n, final Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print("\"");
        printer.print(n.getValue());
        printer.print("\"");
    }

    @Override
    public void visit(final BooleanLiteralExpr n, final Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print(String.valueOf(n.getValue()));
    }

    @Override
    public void visit(final NullLiteralExpr n, final Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print("null");
    }

    @Override
    public void visit(final ThisExpr n, final Void arg) {
        printJavaComment(n.getComment(), arg);
        if (n.getClassExpr().isPresent()) {
            n.getClassExpr().get().accept(this, arg);
            printer.print(".");
        }
        printer.print("this");
    }

    @Override
    public void visit(final SuperExpr n, final Void arg) {
        printJavaComment(n.getComment(), arg);
        if (n.getClassExpr().isPresent()) {
            n.getClassExpr().get().accept(this, arg);
            printer.print(".");
        }
        printer.print("super");
    }

    @Override
    public void visit(final MethodCallExpr n, final Void arg) {
        printJavaComment(n.getComment(), arg);
        if (n.getScope().isPresent()) {
            n.getScope().get().accept(this, arg);
            printer.print(".");
        }
        printTypeArgs(n, arg);
        n.getName().accept(this, arg);
        printArguments(n.getArguments(), arg);
    }

    @Override
    public void visit(final ObjectCreationExpr n, final Void arg) {
        printJavaComment(n.getComment(), arg);
        if (n.getScope().isPresent()) {
            n.getScope().get().accept(this, arg);
            printer.print(".");
        }

        if (!n.getAnonymousClassBody().isPresent()) {
            printer.print("new ");

            printTypeArgs(n, arg);
            if (!isNullOrEmpty(n.getTypeArguments().orElse(null))) {
                printer.print(" ");
            }

            n.getType().accept(this, arg);

            printArguments(n.getArguments(), arg);

        } else {
            boolean isStaticField = false;
            try {
                isStaticField = ((FieldDeclaration)n.getParentNode().get().getParentNode().get()).getModifiers().contains(Modifier.STATIC);
            } catch (Exception ignore) {
            }
            if (isStaticField) {
                printer.print("new class ");
            } else  {
                printer.print("((__this) => new class ");
            }
            boolean isInterface = false;
            Class c = null;
            try {
                c = ClassFinder.findClass(n.getType());
                isInterface = c.isInterface();
//                isInterface = ((ClassOrInterfaceDeclaration)parseClass(n.getType().getName())).isInterface();
            } catch (Exception ignore) {
            }
            if(isInterface){
                printer.print("implements ");
            }else {
                printer.print("extends ");
            }
            printTypeArgs(n, arg);
            if (!isNullOrEmpty(n.getTypeArguments().orElse(null))) {
                printer.print(" ");
            }

            n.getType().accept(this, arg);

            printer.println(" {");
            printer.indent();
            printMembers(n.getAnonymousClassBody().get(), arg);
            printer.unindent();
            printer.print("}");
            printArguments(n.getArguments(), arg);
            if (!isStaticField) {
                printer.print(")(this)");
            }
        }
    }

    @Override
    public void visit(final UnaryExpr n, final Void arg) {
        printJavaComment(n.getComment(), arg);
        if (n.getOperator().isPrefix()) {
            printer.print(n.getOperator().asString());
        }

        n.getExpression().accept(this, arg);

        if (n.getOperator().isPostfix()) {
            printer.print(n.getOperator().asString());
        }
    }

    @Override
    public void visit(final ConstructorDeclaration n, final Void arg) {
        printJavaComment(n.getComment(), arg);
        printMemberAnnotations(n.getAnnotations(), arg);
//        printModifiers(n.getModifiers());

//        printTypeParameters(n.getTypeParameters(), arg);
//        if (n.isGeneric()) {
//            printer.print(" ");
//        }
//        n.getName().accept(this, arg);

        printer.print("constructor(");
        if (!n.getParameters().isEmpty()) {
            for (final Iterator<Parameter> i = n.getParameters().iterator(); i.hasNext(); ) {
                final Parameter p = i.next();
                p.accept(this, arg);
                if (i.hasNext()) {
                    printer.print(", ");
                }
            }
        }
        printer.print(")");

        if (!isNullOrEmpty(n.getThrownExceptions())) {
            warnNotSupport(n, "not support thrown exception here");
//            printer.print(" throws ");
//            for (final Iterator<ReferenceType> i = n.getThrownExceptions().iterator(); i.hasNext(); ) {
//                final ReferenceType name = i.next();
//                name.accept(this, arg);
//                if (i.hasNext()) {
//                    printer.print(", ");
//                }
//            }
        }
        printer.print(" ");
        n.getBody().accept(this, arg);
    }

    @Override
    public void visit(final MethodDeclaration n, final Void arg) {
        printOrphanCommentsBeforeThisChildNode(n);

        printJavaComment(n.getComment(), arg);
        printMemberAnnotations(n.getAnnotations(), arg);
        if (n.getParentNode().get() instanceof ClassOrInterfaceDeclaration
                && ((ClassOrInterfaceDeclaration)n.getParentNode().get()).isInterface()) {
            // don't print modifiers for interface method
        } else {
            printModifiers(n.getModifiers());
        }

        n.getName().accept(this, arg);
        printTypeParameters(n.getTypeParameters(), arg);

        printer.print("(");
        if (!isNullOrEmpty(n.getParameters())) {
            for (final Iterator<Parameter> i = n.getParameters().iterator(); i.hasNext(); ) {
                final Parameter p = i.next();
                p.accept(this, arg);
                if (i.hasNext()) {
                    printer.print(", ");
                }
            }
        }
        printer.print("): ");

        n.getType().accept(this, arg);

        if (!isNullOrEmpty(n.getThrownExceptions())) {
            warnNotSupport(n, "not support thrown exception here");
//            printer.print(" throws ");
//            for (final Iterator<ReferenceType> i = n.getThrownExceptions().iterator(); i.hasNext(); ) {
//                final ReferenceType name = i.next();
//                name.accept(this, arg);
//                if (i.hasNext()) {
//                    printer.print(", ");
//                }
//            }
        }
        if (!n.getBody().isPresent()) {
            printer.print(";");
        } else {
            printer.print(" ");
            n.getBody().get().accept(this, arg);
        }
    }

    @Override
    public void visit(final Parameter n, final Void arg) {
        printJavaComment(n.getComment(), arg);
        printAnnotations(n.getAnnotations(), false, arg);
        printModifiers(n.getModifiers());
        if (n.getModifiers().contains(Modifier.FINAL)) {
            printer.print("const ");
        }
        if (n.isVarArgs()) {
            printAnnotations(n.getVarArgsAnnotations(), false, arg);
            printer.print("...");
        }
        n.getName().accept(this, arg);
        printer.print(": ");
        n.getType().accept(this, arg);
        if (n.isVarArgs()) {
            printer.print("[]");
        }
//        if (!(n.getType() instanceof UnknownType)) {
//            printer.print(" ");
//        }
    }

    @Override
    public void visit(final ExplicitConstructorInvocationStmt n, final Void arg) {
        printJavaComment(n.getComment(), arg);
        if (n.isThis()) {
            printTypeArgs(n, arg);
            printer.print("this");
        } else {
            if (n.getExpression().isPresent()) {
                n.getExpression().get().accept(this, arg);
                printer.print(".");
            }
            printTypeArgs(n, arg);
            printer.print("super");
        }
        printArguments(n.getArguments(), arg);
        printer.print(";");
    }

    @Override
    public void visit(final VariableDeclarationExpr n, final Void arg) {
        printJavaComment(n.getComment(), arg);
        printAnnotations(n.getAnnotations(), false, arg);
        printModifiers(n.getModifiers());

//        if (!n.getVariables().isEmpty()) {
//            n.getMaximumCommonType().accept(this, arg);
//        }
        if (n.getModifiers().contains(Modifier.FINAL)) {
            printer.print("const ");
        } else {
            printer.print("let ");
        }

        for (final Iterator<VariableDeclarator> i = n.getVariables().iterator(); i.hasNext(); ) {
            final VariableDeclarator v = i.next();
            v.accept(this, arg);
            if (i.hasNext()) {
                printer.print(", ");
            }
        }
    }

    @Override
    public void visit(final LocalClassDeclarationStmt n, final Void arg) {
        printJavaComment(n.getComment(), arg);
        n.getClassDeclaration().accept(this, arg);
    }

    @Override
    public void visit(final AssertStmt n, final Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print("assert ");
        n.getCheck().accept(this, arg);
        if (n.getMessage().isPresent()) {
            printer.print(" : ");
            n.getMessage().get().accept(this, arg);
        }
        printer.print(";");
    }

    @Override
    public void visit(final BlockStmt n, final Void arg) {
        printOrphanCommentsBeforeThisChildNode(n);
        printJavaComment(n.getComment(), arg);
        printer.println("{");
        if (n.getStatements() != null) {
            printer.indent();
            for (final Statement s : n.getStatements()) {
                s.accept(this, arg);
                printer.println();
            }
            printer.unindent();
        }
        printOrphanCommentsEnding(n);
        printer.print("}");
    }

    @Override
    public void visit(final LabeledStmt n, final Void arg) {
        printJavaComment(n.getComment(), arg);
        n.getLabel().accept(this, arg);
        printer.print(": ");
        n.getStatement().accept(this, arg);
    }

    @Override
    public void visit(final EmptyStmt n, final Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print(";");
    }

    @Override
    public void visit(final ExpressionStmt n, final Void arg) {
        printOrphanCommentsBeforeThisChildNode(n);
        printJavaComment(n.getComment(), arg);
        n.getExpression().accept(this, arg);
        printer.print(";");
    }

    @Override
    public void visit(final SwitchStmt n, final Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print("switch(");
        n.getSelector().accept(this, arg);
        printer.println(") {");
        if (n.getEntries() != null) {
            printer.indent();
            for (final SwitchEntryStmt e : n.getEntries()) {
                e.accept(this, arg);
            }
            printer.unindent();
        }
        printer.print("}");
    }

    @Override
    public void visit(final SwitchEntryStmt n, final Void arg) {
        printJavaComment(n.getComment(), arg);
        if (n.getLabel().isPresent()) {
            printer.print("case ");
            n.getLabel().get().accept(this, arg);
            printer.print(":");
        } else {
            printer.print("default:");
        }
        printer.println();
        printer.indent();
        if (n.getStatements() != null) {
            for (final Statement s : n.getStatements()) {
                s.accept(this, arg);
                printer.println();
            }
        }
        printer.unindent();
    }

    @Override
    public void visit(final BreakStmt n, final Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print("break");
        n.getLabel().ifPresent(l -> printer.print(" ").print(l.getIdentifier()));
        printer.print(";");
    }

    @Override
    public void visit(final ReturnStmt n, final Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print("return");
        if (n.getExpression().isPresent()) {
            printer.print(" ");
            n.getExpression().get().accept(this, arg);
        }
        printer.print(";");
    }

    @Override
    public void visit(final EnumDeclaration n, final Void arg) {
        printJavaComment(n.getComment(), arg);
        printMemberAnnotations(n.getAnnotations(), arg);
        printModifiers(n.getModifiers());

        printer.print("enum ");
        n.getName().accept(this, arg);

        if (!n.getImplementedTypes().isEmpty()) {
            printer.print(" implements ");
            for (final Iterator<ClassOrInterfaceType> i = n.getImplementedTypes().iterator(); i.hasNext(); ) {
                final ClassOrInterfaceType c = i.next();
                c.accept(this, arg);
                if (i.hasNext()) {
                    printer.print(", ");
                }
            }
        }

        printer.println(" {");
        printer.indent();
        if (n.getEntries() != null) {
            printer.println();
            for (final Iterator<EnumConstantDeclaration> i = n.getEntries().iterator(); i.hasNext(); ) {
                final EnumConstantDeclaration e = i.next();
                e.accept(this, arg);
                if (i.hasNext()) {
                    printer.print(", ");
                }
            }
        }
        if (!n.getMembers().isEmpty()) {
            printer.println(";");
            printMembers(n.getMembers(), arg);
        } else {
            if (!n.getEntries().isEmpty()) {
                printer.println();
            }
        }
        printer.unindent();
        printer.print("}");
    }

    @Override
    public void visit(final EnumConstantDeclaration n, final Void arg) {
        printJavaComment(n.getComment(), arg);
        printMemberAnnotations(n.getAnnotations(), arg);
        n.getName().accept(this, arg);

        if (!n.getArguments().isEmpty()) {
            printArguments(n.getArguments(), arg);
        }

        if (!n.getClassBody().isEmpty()) {
            printer.println(" {");
            printer.indent();
            printMembers(n.getClassBody(), arg);
            printer.unindent();
            printer.println("}");
        }
    }

    @Override
    public void visit(final InitializerDeclaration n, final Void arg) {
        printJavaComment(n.getComment(), arg);
        if (n.isStatic()) {
            printer.print("static ");
        }
        n.getBody().accept(this, arg);
    }

    @Override
    public void visit(final IfStmt n, final Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print("if (");
        n.getCondition().accept(this, arg);
        final boolean thenBlock = n.getThenStmt() instanceof BlockStmt;
        if (thenBlock) // block statement should start on the same line
            printer.print(") ");
        else {
            printer.println(")");
            printer.indent();
        }
        n.getThenStmt().accept(this, arg);
        if (!thenBlock)
            printer.unindent();
        if (n.getElseStmt().isPresent()) {
            if (thenBlock)
                printer.print(" ");
            else
                printer.println();
            final boolean elseIf = n.getElseStmt().orElse(null) instanceof IfStmt;
            final boolean elseBlock = n.getElseStmt().orElse(null) instanceof BlockStmt;
            if (elseIf || elseBlock) // put chained if and start of block statement on a same level
                printer.print("else ");
            else {
                printer.println("else");
                printer.indent();
            }
            if (n.getElseStmt().isPresent())
                n.getElseStmt().get().accept(this, arg);
            if (!(elseIf || elseBlock))
                printer.unindent();
        }
    }

    @Override
    public void visit(final WhileStmt n, final Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print("while (");
        n.getCondition().accept(this, arg);
        printer.print(") ");
        n.getBody().accept(this, arg);
    }

    @Override
    public void visit(final ContinueStmt n, final Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print("continue");
        n.getLabel().ifPresent(l -> printer.print(" ").print(l.getIdentifier()));
        printer.print(";");
    }

    @Override
    public void visit(final DoStmt n, final Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print("do ");
        n.getBody().accept(this, arg);
        printer.print(" while (");
        n.getCondition().accept(this, arg);
        printer.print(");");
    }

    @Override
    public void visit(final ForeachStmt n, final Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print("for (");
        n.getVariable().accept(this, arg);
        printer.print(" : ");
        n.getIterable().accept(this, arg);
        printer.print(") ");
        n.getBody().accept(this, arg);
    }

    @Override
    public void visit(final ForStmt n, final Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print("for (");
        if (n.getInitialization() != null) {
            for (final Iterator<Expression> i = n.getInitialization().iterator(); i.hasNext(); ) {
                final Expression e = i.next();
                e.accept(this, arg);
                if (i.hasNext()) {
                    printer.print(", ");
                }
            }
        }
        printer.print("; ");
        if (n.getCompare().isPresent()) {
            n.getCompare().get().accept(this, arg);
        }
        printer.print("; ");
        if (n.getUpdate() != null) {
            for (final Iterator<Expression> i = n.getUpdate().iterator(); i.hasNext(); ) {
                final Expression e = i.next();
                e.accept(this, arg);
                if (i.hasNext()) {
                    printer.print(", ");
                }
            }
        }
        printer.print(") ");
        n.getBody().accept(this, arg);
    }

    @Override
    public void visit(final ThrowStmt n, final Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print("throw ");
        n.getExpression().accept(this, arg);
        printer.print(";");
    }

    @Override
    public void visit(final SynchronizedStmt n, final Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print("synchronized (");
        n.getExpression().accept(this, arg);
        printer.print(") ");
        n.getBody().accept(this, arg);
    }

    @Override
    public void visit(final TryStmt n, final Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print("try ");
        if (!n.getResources().isEmpty()) {
            printer.print("(");
            Iterator<VariableDeclarationExpr> resources = n.getResources().iterator();
            boolean first = true;
            while (resources.hasNext()) {
                visit(resources.next(), arg);
                if (resources.hasNext()) {
                    printer.print(";");
                    printer.println();
                    if (first) {
                        printer.indent();
                    }
                }
                first = false;
            }
            if (n.getResources().size() > 1) {
                printer.unindent();
            }
            printer.print(") ");
        }
        n.getTryBlock().accept(this, arg);
        for (final CatchClause c : n.getCatchClauses()) {
            c.accept(this, arg);
        }
        if (n.getFinallyBlock().isPresent()) {
            printer.print(" finally ");
            n.getFinallyBlock().get().accept(this, arg);
        }
    }

    @Override
    public void visit(final CatchClause n, final Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print(" catch (");
        n.getParameter().accept(this, arg);
        printer.print(") ");
        n.getBody().accept(this, arg);
    }

    @Override
    public void visit(final AnnotationDeclaration n, final Void arg) {
        printJavaComment(n.getComment(), arg);
        printMemberAnnotations(n.getAnnotations(), arg);
        printModifiers(n.getModifiers());

        printer.print("@interface ");
        n.getName().accept(this, arg);
        printer.println(" {");
        printer.indent();
        if (n.getMembers() != null) {
            printMembers(n.getMembers(), arg);
        }
        printer.unindent();
        printer.print("}");
    }

    @Override
    public void visit(final AnnotationMemberDeclaration n, final Void arg) {
        printJavaComment(n.getComment(), arg);
        printMemberAnnotations(n.getAnnotations(), arg);
        printModifiers(n.getModifiers());

        n.getType().accept(this, arg);
        printer.print(" ");
        n.getName().accept(this, arg);
        printer.print("()");
        if (n.getDefaultValue().isPresent()) {
            printer.print(" default ");
            n.getDefaultValue().get().accept(this, arg);
        }
        printer.print(";");
    }

    @Override
    public void visit(final MarkerAnnotationExpr n, final Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print("/* @");
        n.getName().accept(this, arg);
        printer.print(" */");
    }

    @Override
    public void visit(final SingleMemberAnnotationExpr n, final Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print("/* @");
        n.getName().accept(this, arg);
        printer.print("(");
        n.getMemberValue().accept(this, arg);
        printer.print(") */");
    }

    @Override
    public void visit(final NormalAnnotationExpr n, final Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print("/* @");
        n.getName().accept(this, arg);
        printer.print("(");
        if (n.getPairs() != null) {
            for (final Iterator<MemberValuePair> i = n.getPairs().iterator(); i.hasNext(); ) {
                final MemberValuePair m = i.next();
                m.accept(this, arg);
                if (i.hasNext()) {
                    printer.print(", ");
                }
            }
        }
        printer.print(") */");
    }

    @Override
    public void visit(final MemberValuePair n, final Void arg) {
        printJavaComment(n.getComment(), arg);
        n.getName().accept(this, arg);
        printer.print(" = ");
        n.getValue().accept(this, arg);
    }

    @Override
    public void visit(final LineComment n, final Void arg) {
        if (configuration.isIgnoreComments()) {
            return;
        }
        printer.print("//");
        String tmp = n.getContent();
        tmp = tmp.replace('\r', ' ');
        tmp = tmp.replace('\n', ' ');
        printer.println(tmp);
    }

    @Override
    public void visit(final BlockComment n, final Void arg) {
        if (configuration.isIgnoreComments()) {
            return;
        }
        printer.print("/*").print(n.getContent()).println("*/");
    }

    @Override
    public void visit(LambdaExpr n, Void arg) {
        printJavaComment(n.getComment(), arg);

        final NodeList<Parameter> parameters = n.getParameters();
        final boolean printPar = n.isEnclosingParameters();

        if (printPar) {
            printer.print("(");
        }
        for (Iterator<Parameter> i = parameters.iterator(); i.hasNext(); ) {
            Parameter p = i.next();
            p.accept(this, arg);
            if (i.hasNext()) {
                printer.print(", ");
            }
        }
        if (printPar) {
            printer.print(")");
        }

        printer.print(" => ");
        final Statement body = n.getBody();
        if (body instanceof ExpressionStmt) {
            // Print the expression directly
            ((ExpressionStmt) body).getExpression().accept(this, arg);
        } else {
            body.accept(this, arg);
        }
    }

    @Override
    public void visit(MethodReferenceExpr n, Void arg) {
        printJavaComment(n.getComment(), arg);
        Expression scope = n.getScope();
        String identifier = n.getIdentifier();
        if (scope != null) {
            n.getScope().accept(this, arg);
        }

        printer.print("::");
        printTypeArgs(n, arg);
        if (identifier != null) {
            printer.print(identifier);
        }
    }

    @Override
    public void visit(TypeExpr n, Void arg) {
        printJavaComment(n.getComment(), arg);
        if (n.getType() != null) {
            n.getType().accept(this, arg);
        }
    }

    @Override
    public void visit(NodeList n, Void arg) {
        for (Object node : n) {
            ((Node) node).accept(this, arg);
        }
    }

    @Override
    public void visit(final ImportDeclaration n, final Void arg) {
        printJavaComment(n.getComment(), arg);
        if (n.isAsterisk()) {
            // TODO
            warnNotSupport(n, "not support Asterisk in import");
            return;
        }
        printer.print("import " + n.getName().getIdentifier() + " from \"");
//        if (n.isStatic()) {
//            printer.print("static ");
//        }
        n.getName().accept(this, arg);
        printer.println("\";");

        printOrphanCommentsEnding(n);
    }


    @Override
    public void visit(ModuleDeclaration n, Void arg) {
        printAnnotations(n.getAnnotations(), false, arg);
        printer.println();
        if (n.isOpen()) {
            printer.print("open ");
        }
        printer.print("module ");
        n.getName().accept(this, arg);
        printer.println(" {").indent();
        n.getModuleStmts().accept(this, arg);
        printer.unindent().println("}");
    }

    @Override
    public void visit(ModuleRequiresStmt n, Void arg) {
        printer.print("requires ");
        printModifiers(n.getModifiers());
        n.getName().accept(this, arg);
        printer.println(";");
    }

    @Override
    public void visit(ModuleExportsStmt n, Void arg) {
        printer.print("exports ");
        n.getName().accept(this, arg);
        printPrePostFixOptionalList(n.getModuleNames(), arg, " to ", ", ", "");
        printer.println(";");
    }

    @Override
    public void visit(ModuleProvidesStmt n, Void arg) {
        printer.print("provides ");
        n.getType().accept(this, arg);
        printPrePostFixRequiredList(n.getWithTypes(), arg, " with ", ", ", "");
        printer.println(";");
    }

    @Override
    public void visit(ModuleUsesStmt n, Void arg) {
        printer.print("uses ");
        n.getType().accept(this, arg);
        printer.println(";");
    }

    @Override
    public void visit(ModuleOpensStmt n, Void arg) {
        printer.print("opens ");
        n.getName().accept(this, arg);
        printPrePostFixOptionalList(n.getModuleNames(), arg, " to ", ", ", "");
        printer.println(";");
    }

    @Override
    public void visit(UnparsableStmt n, Void arg) {
        printer.print("???;");
    }

    private void printOrphanCommentsBeforeThisChildNode(final Node node) {
        if (configuration.isIgnoreComments()) return;
        if (node instanceof Comment) return;

        Node parent = node.getParentNode().orElse(null);
        if (parent == null) return;
        List<Node> everything = new LinkedList<>();
        everything.addAll(parent.getChildNodes());
        sortByBeginPosition(everything);
        int positionOfTheChild = -1;
        for (int i = 0; i < everything.size(); i++) {
            if (everything.get(i) == node) positionOfTheChild = i;
        }
        if (positionOfTheChild == -1) {
            throw new AssertionError("I am not a child of my parent.");
        }
        int positionOfPreviousChild = -1;
        for (int i = positionOfTheChild - 1; i >= 0 && positionOfPreviousChild == -1; i--) {
            if (!(everything.get(i) instanceof Comment)) positionOfPreviousChild = i;
        }
        for (int i = positionOfPreviousChild + 1; i < positionOfTheChild; i++) {
            Node nodeToPrint = everything.get(i);
            if (!(nodeToPrint instanceof Comment))
                throw new RuntimeException(
                        "Expected comment, instead " + nodeToPrint.getClass() + ". Position of previous child: "
                                + positionOfPreviousChild + ", position of child " + positionOfTheChild);
            nodeToPrint.accept(this, null);
        }
    }

    private void printOrphanCommentsEnding(final Node node) {
        if (configuration.isIgnoreComments()) return;

        List<Node> everything = new LinkedList<>();
        everything.addAll(node.getChildNodes());
        sortByBeginPosition(everything);
        if (everything.isEmpty()) {
            return;
        }

        int commentsAtEnd = 0;
        boolean findingComments = true;
        while (findingComments && commentsAtEnd < everything.size()) {
            Node last = everything.get(everything.size() - 1 - commentsAtEnd);
            findingComments = (last instanceof Comment);
            if (findingComments) {
                commentsAtEnd++;
            }
        }
        for (int i = 0; i < commentsAtEnd; i++) {
            everything.get(everything.size() - commentsAtEnd + i).accept(this, null);
        }
    }

}
