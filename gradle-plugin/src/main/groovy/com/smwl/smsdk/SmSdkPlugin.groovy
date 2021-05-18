package com.smwl.smsdk

import com.android.build.gradle.AppExtension
import com.android.build.gradle.internal.api.ApplicationVariantImpl
import com.android.build.gradle.tasks.MergeSourceSetFolders
import org.apache.commons.codec.digest.DigestUtils
import org.apache.http.util.TextUtils
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

import java.text.SimpleDateFormat

/**
 * Created on 2021/5/12.
 *
 * @author Linzugeng
 */
public class SmSdkPlugin implements Plugin<Project> {

    private static final GRADLE_3_1_0 = "gradleVersion3.1.0"

    private Project project

    @Override
    public void apply(Project project) {
        this.project = project

        //宿主必须是application
        //The target project must be a android application module
        if (!project.plugins.hasPlugin('com.android.application')) {
            throw new Exception('"com.smwl.smsdk.plugin" required "com.android.application"!')
        }

        detectGradleVersion()

        project.extensions.create("smsdk", SmSdkExtension)

        project.afterEvaluate {
            Log.i "afterEvaluate"
            checkConfig()

            checkCode()

            android.applicationVariants.each { ApplicationVariantImpl variant ->
//                Log.i "afterEvaluate", "variant.name.capitalize() = ${variant.name.capitalize()}"
                String processManifestTaskName = variant.variantData.scope.getTaskName("process", "Manifest")
                Task processTask = project.tasks[processManifestTaskName]
                String reportFileName
                File mergerReportFile
                processTask.doLast {
                    String variantBaseName = processTask.variantConfiguration.baseName
                    reportFileName = "manifest-merger-${variantBaseName}-report.txt"
                    String mergerReportDir = "${project.getBuildDir()}/outputs/logs"
//                    Log.i "mergerReportDir = $mergerReportDir"
                    mergerReportFile = new File(mergerReportDir + File.separator + reportFileName)
                    if (mergerReportFile != null && mergerReportFile.exists()) {
                        Log.i "mergerReportFile is exist. file: ${reportFileName}"
                    }
                }

                String mergeAssetsTaskName = variant.variantData.scope.getTaskName("merge", "Assets")
                Task task = project.tasks[mergeAssetsTaskName]
                if (task in MergeSourceSetFolders) {
                    MergeSourceSetFolders mergeAssetsTask = task as MergeSourceSetFolders
                    mergeAssetsTask.doLast {
                        String mergeAssetsOutputDirName
                        if (project.extensions.extraProperties.get(GRADLE_3_1_0)) {
                            mergeAssetsOutputDirName = mergeAssetsTask.outputDir.get().asFile.path
                        } else {
                            mergeAssetsOutputDirName = mergeAssetsTask.outputDir.path
                        }
                        Log.i "mergeAssetsOutputDirName = ${mergeAssetsOutputDirName}"
                        File log = createLogFile(mergeAssetsOutputDirName + "/smsdk-log.txt")
                        if (mergerReportFile != null && mergerReportFile.exists()) {
                            project.copy {
                                Log.i "copy"
                                from mergerReportFile
                                into mergeAssetsOutputDirName
                                rename { "smsdk-manifest-merger-report.txt" }
                                writeInfoLog(log, "Manifest merger report is added! file: ${reportFileName}")
                            }
                        } else {
                            Log.e "Manifest merger report file not found!"
                            writeErrorLog(log, "Manifest merger report not found! file: ${reportFileName}")
                        }
                    }
                }
            }
        }
    }

    private final void detectGradleVersion() {
        project.ext.set(GRADLE_3_1_0, false)

        try {
            //通过显式加载类，判断gradle版本是否为3.1.0以上
            Class.forName('com.android.builder.core.VariantConfiguration')
        } catch (Throwable ignored) {
            //3.1.0后包名已变成com.android.build.gradle.internal.core.VariantConfiguration
            project.ext.set(GRADLE_3_1_0, true)
        }
    }

    private final String checkCode() {
        def appKeyMd5 = md5(smSdk.appKey)
        Log.i "checkCode appKey = $smSdk.appKey"
        Log.i "checkCode appKeyMd5 = $appKeyMd5"
        android.defaultConfig.buildConfigField("String", "CHECK_CODE", "\"$appKeyMd5\"")
    }

    private static String md5(String str) {
        return String.format("%032x", new BigInteger(1, DigestUtils.md5(str)))
    }

    private void checkConfig() {
        if (TextUtils.isEmpty(smSdk.appKey)) {
            def err = new StringBuilder('you should set the appKey in build.gradle,\n ')
            err.append('please declare it in application project build.gradle:\n')
            err.append('    smsdk {\n')
            err.append('        appKey = "xxxxx" \n')
            err.append('    }\n')
            throw new InvalidUserDataException(err.toString())
        }
    }

    private final SmSdkExtension getSmSdk() {
        return this.project.extensions.findByName("smsdk")
    }

    private final AppExtension getAndroid() {
        return this.project.android
    }

    private File createLogFile(String logFileFullName) {
        def file = project.file(logFileFullName)
        file.createNewFile()
        file.append("gradleVersion: ${project.gradle.gradleVersion}")
        file.append("\napplicationId: ${android.defaultConfig.applicationId}")
        file.append("\nminSdkVersion: ${android.defaultConfig.minSdkVersion.apiString}")
        file.append("\ntargetSdkVersion: ${android.defaultConfig.targetSdkVersion.apiString}")
        file.append("\nsourceCompatibility: ${android.compileOptions["sourceCompatibility"]}")
        file.append("\ntargetCompatibility: ${android.compileOptions["targetCompatibility"]}")
        return file
    }

    private static void writeInfoLog(File file, String info) {
        def timeTag = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())
        file.append("\n\n[INFO][$timeTag] $info")
    }

    private static void writeErrorLog(File file, String error) {
        def timeTag = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())
        file.append("\n\n[ERROR][$timeTag] $error")
    }
}
