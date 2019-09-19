/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package org.blade.build;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import org.apache.commons.io.FileUtils;

import org.gradle.api.*;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.jvm.tasks.Jar;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

public class BladeBuildPlugin implements Plugin<Project> {

    private static final String BLADE_SDK_VERSION = "1.0.2";

    public void apply(Project project) throws GradleException {
        var deps = project.getDependencies();
        deps.add("implementation", "org.bladetools:bladelib:" + BLADE_SDK_VERSION);

        JavaPluginConvention javaPluginConvention = project.getConvention().getPlugin(JavaPluginConvention.class);
        javaPluginConvention.setSourceCompatibility(JavaVersion.VERSION_1_7);
        javaPluginConvention.setTargetCompatibility(JavaVersion.VERSION_1_7);

        checkSourcePass(project);
        convertToDex(project);
    }

    private void convertToDex(Project project) throws GradleException {
        Jar jar = (Jar)project.getTasks().findByName("jar");
        if (jar == null)
            return;

        jar.doFirst(task -> {
            jar.getManifest().getAttributes().put("Blade-SDK", BLADE_SDK_VERSION);
        });

        jar.doLast(task -> {
            String jFile = jar.getArchiveFile().get().getAsFile().getAbsolutePath();
            String[] args = new String[]{"--dex", "--output=" + jFile, jFile};
            try {
                com.android.dx.command.Main.main(args);
            } catch (Exception e) {
                throw new GradleException("Unable convert jar to dex. " + e.getMessage());
            }
        });
    }

    private void generateManifestJson(Project project) {
        BladePluginExtension extension = project.getExtensions().create("blade", BladePluginExtension.class);

        Task classes = project.getTasks().findByName("classes");
        if (classes == null)
            return;

        classes.doLast(task -> {
            checkManifest(extension);

            JavaPluginConvention javaPluginConvention = project.getConvention().getPlugin(JavaPluginConvention.class);
            SourceSet main = javaPluginConvention.getSourceSets().getByName("main");
            File manifest = new File(main.getOutput().getResourcesDir(), "manifest.json");
            writeManifest(manifest, extension);
        });
    }

    private void writeManifest(File manifest, BladePluginExtension extension) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            SimpleBeanPropertyFilter filter = SimpleBeanPropertyFilter.filterOutAllExcept("name", "author", "version", "packageNames", "install", "coldStartOnly");
            objectMapper.setFilterProvider(new SimpleFilterProvider().addFilter("manifest_filter", filter));

            FileUtils.write(manifest, objectMapper.writeValueAsString(extension), "utf-8");
        } catch (Exception e) {
            throw new GradleException("Unable to generate manifest.json. " + e.getMessage());
        }
    }

    private void checkManifest(BladePluginExtension extension) throws GradleException {
        if (extension.name == null || extension.name.length() == 0)
            throw new GradleException("Invalid value. (blade.name)");
        if (extension.author == null || extension.author.length() == 0)
            throw new GradleException("Invalid value. (blade.author)");
        if (extension.version == null || extension.version.length() == 0)
            throw new GradleException("Invalid value. (blade.version)");
        if (extension.packageNames == null || extension.packageNames.length == 0)
            throw new GradleException("Invalid module name. (blade.packageNames)");
        if (extension.install == null || extension.install.length == 0)
            throw new GradleException("Invalid module name. (blade.install)");
    }

    private void checkSourcePass(Project project) {
        Task compileJava = project.getTasks().findByName("compileJava");

        if (compileJava == null)
            return;

        compileJava.doLast(task -> {
            JavaPluginConvention javaPluginConvention = project.getConvention().getPlugin(JavaPluginConvention.class);
            SourceSet main = javaPluginConvention.getSourceSets().getByName("main");
            main.java(files -> files.forEach(file -> {
                checkSource(file);
            }));
        });
    }

    private void checkSource(File java) throws GradleException {
        try {
            CompilationUnit compilationUnit = StaticJavaParser.parse(java);
            List<MethodDeclaration> swapMethods = compilationUnit.findAll(MethodDeclaration.class).stream().
                    filter(m ->m.isAnnotationPresent("BladeSwap")).collect(Collectors.toList());
            for (MethodDeclaration sm : swapMethods) {
                String from = null;

                Optional<AnnotationExpr> anno = sm.getAnnotationByName("BladeSwap");
                if (anno.isPresent() && anno.get().isSingleMemberAnnotationExpr()) {
                    from = anno.get().asSingleMemberAnnotationExpr().getMemberValue().asStringLiteralExpr().asString();
                }

                if (from == null)
                    continue;

                Optional<Node> clazz = sm.getParentNode();
                if (!clazz.isEmpty()) {
                    boolean matched = false;
                    List<MethodDeclaration> cm = clazz.get().findAll(MethodDeclaration.class);
                    for (MethodDeclaration m : cm) {
                        if (cm == sm)
                            continue;
                        if (hasSameSignature(sm, m) && m.getNameAsString().equals(from)) {
                            matched = true;
                            break;
                        }
                    }

                    if (!matched)
                        throw new GradleException(String.format(Locale.getDefault(), "Method %s not found", from));
                }
            }
        } catch (FileNotFoundException e) {
            throw new GradleException("Unable to parse source " + java.getAbsolutePath());
        }
    }

    private boolean hasSameSignature(MethodDeclaration m1, MethodDeclaration m2) {
        if (!m1.getType().equals(m2.getType()))
            return false;

        if (m1.getParameters().size() != m2.getParameters().size())
            return false;

        for (int i = 0 ; i < m1.getParameters().size(); ++i) {
            if (!m1.getParameter(i).getType().equals(m2.getParameter(i).getType()))
                return false;
        }

        return true;
    }
}
