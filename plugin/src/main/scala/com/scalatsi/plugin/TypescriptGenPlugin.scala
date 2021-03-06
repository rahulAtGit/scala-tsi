package com.scalatsi.plugin

import sbt.Keys._
import sbt._
import sbt.info.BuildInfo

object TypescriptGenPlugin extends AutoPlugin {
  object autoImport {
    val generateTypescript =
      taskKey[Unit]("Generate typescript this project")
    val generateTypescriptGeneratorApplication =
      taskKey[Seq[File]]("Generate an application that will generate typescript from the classes that are configured")
    val typescriptClassesToGenerateFor =
      settingKey[Seq[String]]("Classes to generate typescript interfaces for")
    val typescriptGenerationImports =
      settingKey[Seq[String]]("Additional imports (i.e. your packages so you don't need to prefix your classes)")
    val typescriptOutputFile      = settingKey[File]("File where all typescript interfaces will be written to")
    val typescriptStyleSemicolons = settingKey[Boolean]("Whether to add booleans to the exported model")
  }

  import autoImport._

  override def trigger = allRequirements

  private val scala_ts_compiler_version = BuildInfo.version

  override lazy val projectSettings = Seq(
    // User settings
    libraryDependencies += "com.scalatsi" %% "scala-tsi" % scala_ts_compiler_version,
    typescriptGenerationImports := Seq(),
    typescriptClassesToGenerateFor := Seq(),
    typescriptOutputFile := target.value / "scala-interfaces.ts",
    typescriptStyleSemicolons := false,
    // Task settings
    generateTypescript := runTypescriptGeneration.value,
    generateTypescriptGeneratorApplication in Compile := createTypescriptGenerationTemplate(
      typescriptGenerationImports.value,
      typescriptClassesToGenerateFor.value,
      sourceManaged.value,
      typescriptOutputFile.value,
      typescriptStyleSemicolons.value
    ),
    sourceGenerators in Compile += generateTypescriptGeneratorApplication in Compile
  )

  def createTypescriptGenerationTemplate(
    imports: Seq[String],
    typesToGenerate: Seq[String],
    sourceManaged: File,
    typescriptOutputFile: File,
    useSemicolons: Boolean
  ): Seq[File] = {
    val targetFile = sourceManaged / "com" / "scalatsi" / "generator" / "ApplicationTypescriptGeneration.scala"

    val toWrite: String = txt
      .generateTypescriptApplicationTemplate(
        imports,
        typesToGenerate,
        typescriptOutputFile.getAbsolutePath,
        useSemicolons
      )
      .body
      .stripMargin

    IO.write(targetFile, toWrite)
    Seq(targetFile)
  }

  def runTypescriptGeneration: Def.Initialize[Task[Unit]] =
    (runMain in Compile)
      .toTask(" com.scalatsi.generator.ApplicationTypescriptGeneration")
      .dependsOn(generateTypescriptGeneratorApplication in Compile)
}
