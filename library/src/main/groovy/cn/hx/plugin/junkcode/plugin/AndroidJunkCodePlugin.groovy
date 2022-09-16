package cn.hx.plugin.junkcode.plugin

import cn.hx.plugin.junkcode.ext.AndroidJunkCodeExt
import cn.hx.plugin.junkcode.ext.JunkCodeConfig
import cn.hx.plugin.junkcode.task.AndroidJunkCodeTask
import com.android.build.gradle.AppExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidJunkCodePlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        def android = project.extensions.findByType(AppExtension)
        if (!android) {
            throw IllegalArgumentException("must apply this plugin after 'com.android.application'")
        }
        def generateJunkCodeExt = project.extensions.create("androidJunkCode", AndroidJunkCodeExt)
        generateJunkCodeExt.variantConfig = project.container(JunkCodeConfig.class, new JunkCodeConfigFactory())
        android.applicationVariants.all { variant ->
            def variantName = variant.name

            def junkCodeConfig = generateJunkCodeExt.variantConfig.findByName(variantName)
            if (junkCodeConfig) {
                createGenerateJunkCodeTask(project, android, variant, junkCodeConfig)
            } else {
                if(variantName.toLowerCase().contains("debug")) { //debug 不要加
                    return
                }
                Random random = new Random();
                junkCodeConfig = new JunkCodeConfig(variantName)
                junkCodeConfig.packageBase = createPackageName()
                junkCodeConfig.packageCount = 50 + random.nextInt(10)
                junkCodeConfig.activityCountPerPackage = 5 + +random.nextInt(10)
                junkCodeConfig.excludeActivityJavaFile = false
                junkCodeConfig.otherCountPerPackage = 60 + +random.nextInt(10)
                junkCodeConfig.methodCountPerClass = 23 + +random.nextInt(10)
                junkCodeConfig.resPrefix = createResPrefix()
                junkCodeConfig.drawableCount = 300 + +random.nextInt(100)
                junkCodeConfig.stringCount = 300 + +random.nextInt(100)
                createGenerateJunkCodeTask(project, android, variant, junkCodeConfig)
            }
        }
    }


    private String createPackageName() {
        return randStr(4) + "." + randStr(5) + "." + randStr(6) + "." + randStr(3)
    }

    private String createResPrefix() {
        return randStr(15) + "_"
    }

    private String randStr(int num) {
        String str = "abcdefghijklmnopqrstuvwxyz"
        StringBuffer buffer = new StringBuffer()
        Random random = new Random()
        for (i in 0..<num) {
            int n = random.nextInt(str.length())
            buffer.append(str.charAt(n))
        }
        return buffer.toString()
    }


    private def createGenerateJunkCodeTask = { project, android, variant, junkCodeConfig ->
        def variantName = variant.name
        def generateJunkCodeTaskName = "generate${variantName.capitalize()}JunkCode"
        def dir = new File(project.buildDir, "generated/source/junk/$variantName")
        def resDir = new File(dir, "res")
        def javaDir = new File(dir, "java")
        def manifestFile = new File(dir, "AndroidManifest.xml")
        //从main/AndroidManifest.xml找到package name
        def mainManifestFile = android.sourceSets.findByName("main").manifest.srcFile
        def parser = new XmlParser()
        def node = parser.parse(mainManifestFile)
        def packageName = node.attribute("package")
        def generateJunkCodeTask = project.task(generateJunkCodeTaskName, type: AndroidJunkCodeTask) {
            config = junkCodeConfig
            manifestPackageName = packageName
            outDir = dir
        }
        //将自动生成的AndroidManifest.xml加入到一个未被占用的manifest位置(如果都占用了就不合并了，通常较少出现全被占用情况)
        for (int i = variant.sourceSets.size() - 1; i >= 0; i--) {
            def sourceSet = variant.sourceSets[i]
            if (!sourceSet.manifestFile.exists()) {
                sourceSet.manifest.srcFile(manifestFile)
                break
            }
        }
        if (variant.respondsTo("registerGeneratedResFolders")) {
            variant.registerGeneratedResFolders(project
                    .files(resDir)
                    .builtBy(generateJunkCodeTask))
            if (variant.hasProperty("mergeResourcesProvider")) {
                variant.mergeResourcesProvider.configure { dependsOn(generateJunkCodeTask) }
            } else {
                //noinspection GrDeprecatedAPIUsage
                variant.mergeResources.dependsOn(generateJunkCodeTask)
            }
        } else {
            //noinspection GrDeprecatedAPIUsage
            variant.registerResGeneratingTask(generateJunkCodeTask, resDir)
        }
        variant.registerJavaGeneratingTask(generateJunkCodeTask, javaDir)
    }
}