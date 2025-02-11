/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.maven;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.apache.commons.text.StringEscapeUtils;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.tools.ant.taskdefs.Expand;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.types.Environment;
import org.apache.tools.ant.types.Path;

/**
 * A mojo that should be called sometime before simulator runs.  It sets properties
 * that the simulator requires to run properly, including:
 * codename1.mainClass
 * codename1.css.compiler.args.input
 * codename1.css.compiler.args.output
 * codename1.css.compiler.args.output
 *
 * @author shannah
 */
@Mojo(name = "prepare-simulator-classpath", defaultPhase = LifecyclePhase.INITIALIZE, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class PrepareSimulatorClasspathMojo extends AbstractCN1Mojo {

    private boolean filterByName(Artifact artifact) {
        if (GROUP_ID.equals(artifact.getGroupId())) {
            if ("java-runtime".equals(artifact.getArtifactId())) {
                return false;
            }
        }
        return true;
    }

    private String prepareClasspath() {
        StringBuilder sb = new StringBuilder();
        try {
            for (String el : project.getRuntimeClasspathElements()) {
                if (sb.length() > 0) {
                    sb.append(File.pathSeparator);
                }
                sb.append(el);
            }
        } catch (Exception ex) {
            getLog().error("Failed to get runtime classpath elementes", ex);
        }
        return sb.toString();
    }


    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {
        getLog().info("Preparing Simulator Classpath");
        Properties props = project.getModel().getProperties();
        if (props == null) {
            props = new Properties();
            project.getModel().setProperties(props);
        }
        //props.setProperty("exec.args", properties.getProperty("codename1.packageName")+"."+properties.getProperty("codename1.mainName"));
        project.getModel().addProperty("codename1.mainClass", properties.getProperty("codename1.packageName")+"."+properties.getProperty("codename1.mainName"));
        // Setup CEF directory
        if (!isCefSetup()) {
            getLog().debug("CEF Not set up yet.  Setting it up now");
            setupCef();
        } else {
            getLog().debug("CEF is already set up. cef.dir="+System.getProperty("cef.dir"));
        }
        project.getProperties().setProperty("cef.dir", System.getProperty("cef.dir"));
        
        File designerJar = getDesignerJar();
        
        if (designerJar != null && designerJar.exists()) {
            project.getModel().addProperty("codename1.designer.jar", designerJar.getAbsolutePath());
        } else {
            throw new MojoExecutionException("Can't find designer jar");
        }
        
        File cssFile = new File(getCN1ProjectDir(), "src" + File.separator + "main" + File.separator + "css" + File.separator + "theme.css");
        File resFile = new File(getCN1ProjectDir(), "target" + File.separator + "classes" + File.separator + "theme.res");
        File mergeFile = new File(getCN1ProjectDir(), "target" + File.separator + "css" + File.separator + "theme.css");

        if (cssFile.exists()) {
            project.getModel().addProperty("codename1.css.compiler.args.input", cssFile.getAbsolutePath());
            project.getModel().addProperty("codename1.css.compiler.args.output", resFile.getAbsolutePath());
            project.getModel().addProperty("codename1.css.compiler.args.merge", mergeFile.getAbsolutePath());
        } else {
            project.getModel().addProperty("codename1.css.compiler.args.input", "");
            project.getModel().addProperty("codename1.css.compiler.args.output", "");
            project.getModel().addProperty("codename1.css.compiler.args.merge", "");
        }
        if ("true".equals(project.getProperties().getProperty("cn1.class.path.required"))) {
            project.getModel().addProperty("cn1.class.path", prepareClasspath());
        }

        
    }
    
    
}
