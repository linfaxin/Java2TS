package print;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import util.ClassUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by faxin on 2017/8/28.
 */
public class ClassFinder {
    public static Class findClass(ClassOrInterfaceType type) throws Exception {
        ArrayList<String> scopeAndTypes = new ArrayList<>();
        scopeAndTypes.add(type.getNameAsString());
        ClassOrInterfaceType find = type;
        while (find.getScope().isPresent()) {
            find = find.getScope().get();
            scopeAndTypes.add(0, find.getNameAsString());
        }
        Class rootScopeClass = findScopeClass(find);
        scopeAndTypes.set(0, rootScopeClass.getName());
        return Class.forName(scopeAndTypes.stream().collect(Collectors.joining("$")));
    }
    private static Class findScopeClass(ClassOrInterfaceType type) throws Exception {
        String typeName = type.getNameAsString();
        // find in inner class
        ClassOrInterfaceDeclaration findInClass = findOwnerClassDeclar(type);
        while (findInClass != null) {
            for (ClassOrInterfaceDeclaration c : getDirectClassDeclarChildren(findInClass)) {
                if (c.getNameAsString().equals(typeName)) {
                    return Class.forName(getFullClassName(c));
                }
            }
            findInClass = findOwnerClassDeclar(findInClass);
        }
        // find in import
        CompilationUnit root = findRootUnit(type);
        if (root != null && root.getImports() != null) {
            for (ImportDeclaration imp : root.getImports()) {
                String nameWithPackage = imp.getName().getQualifier() + "." + imp.getName().getIdentifier();
                if (imp.isAsterisk()) {
                    try {
                        return Class.forName(nameWithPackage + "." + typeName);
                    } catch (Exception ignore) {
                    }
                } else if (imp.getName().getIdentifier().equals(typeName)) {
                    return Class.forName(nameWithPackage);
                }
            }
        }
        // find in same package
        if (root != null && root.getPackageDeclaration().isPresent()) {
            String packageName = root.getPackageDeclaration().get().getNameAsString();
            for (Class packageClass : ClassUtil.getClasses(packageName)) {
                if (packageClass.getSimpleName().equals(typeName)) {
                    return packageClass;
                }
            }
        }

        // find in java.lang package
        try {
            return Class.forName("java.lang." + typeName);
        } catch (Exception ignore) {
        }

        return null;
    }
    public static String getFullClassName(ClassOrInterfaceDeclaration innerClass) {
        ArrayList<String> classNames = new ArrayList<>();
        ClassOrInterfaceDeclaration checkClass = innerClass;
        while (checkClass != null) {
            classNames.add(0, checkClass.getNameAsString());
            Node parent = checkClass.getParentNode().orElse(null);
            if (parent instanceof ClassOrInterfaceDeclaration) {
                checkClass = (ClassOrInterfaceDeclaration) parent;
            } else {
                break;
            }
        }
        String classNameWithOutPackage = classNames.stream().collect(Collectors.joining("$"));
        CompilationUnit root = findRootUnit(innerClass);
        if (root != null && root.getPackageDeclaration().isPresent()) {
            return root.getPackageDeclaration().get().getNameAsString() + "." + classNameWithOutPackage;
        }
        return classNameWithOutPackage;
    }
    private static CompilationUnit findRootUnit(Node node) {
        Node parent = node.getParentNode().orElse(null);
        if (parent != null) {
            if (parent instanceof CompilationUnit) {
                return (CompilationUnit) parent;
            } else {
                return findRootUnit(parent);
            }
        }
        return null;
    }
    private static ClassOrInterfaceDeclaration findOwnerClassDeclar(Node node) {
        Node parent = node.getParentNode().orElse(null);
        if (parent != null) {
            if (parent instanceof ClassOrInterfaceDeclaration) {
                return (ClassOrInterfaceDeclaration) parent;
            } else {
                return findOwnerClassDeclar(parent);
            }
        }
        return null;
    }
    private static List<ClassOrInterfaceDeclaration> getDirectClassDeclarChildren(ClassOrInterfaceDeclaration parent) {
        return parent.getMembers().stream()
                .filter(bodyDeclaration -> bodyDeclaration instanceof ClassOrInterfaceDeclaration)
                .map(bodyDeclaration -> (ClassOrInterfaceDeclaration) bodyDeclaration)
                .collect(Collectors.toList());
    }
}
