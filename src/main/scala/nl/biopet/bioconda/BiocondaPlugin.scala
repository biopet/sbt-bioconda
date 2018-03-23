package nl.biopet.bioconda

import java.io.{FileInputStream, PrintWriter}

import com.roundeights.hasher.Implicits._
import com.typesafe.sbt.GitPlugin
import com.typesafe.sbt.SbtGit.GitKeys
import com.typesafe.sbt.git.GitRunner
import ohnosequences.sbt.GithubRelease.keys.{TagName, ghreleaseGetRepo}
import ohnosequences.sbt.SbtGithubReleasePlugin
import org.kohsuke.github.{GHAsset, GHRelease, GHRepository}
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.Constructor
import sbt.Keys._
import sbt.Scoped.AnyInitTask
import sbt.internal.util.ManagedLogger
import sbt.{Def, _}

import scala.collection.JavaConverters
import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import scala.language.postfixOps

object BiocondaPlugin extends AutoPlugin {
  override def trigger: PluginTrigger = allRequirements

  override def requires: Plugins = {
    GitPlugin && SbtGithubReleasePlugin
  }

  object autoImport extends BiocondaKeys

  import autoImport._

  override def projectSettings: Seq[Setting[_]] = Def.settings(
    biocondaBranch := (normalizedName in Bioconda).value,
    biocondaMainGitUrl := "https://github.com/bioconda/bioconda-recipes.git",
    biocondaMainBranch := "master",
    biocondaUpdatedRepository := initBiocondaRepo.value,
    biocondaUpdatedBranch := updateBranch()
      .dependsOn(biocondaUpdatedRepository)
      .value,
    biocondaRepository := new File(target.value, "bioconda"),
    biocondaRecipeDir := new File(target.value, "recipes"),
    biocondaBuildNumber := 0,
    biocondaRequirements := Seq("openjdk"),
    biocondaBuildRequirements := Seq(),
    biocondaNotes := defaultNotes.value,
    biocondaSummary := defaultSummary.value,
    biocondaDefaultJavaOptions := Seq(),
    biocondaCreateVersionRecipes := createVersionRecipes.value,
    biocondaCreateLatestRecipe := createLatestRecipes.value,
    biocondaCreateRecipes := createLatestRecipes.dependsOn(createVersionRecipes).value,
    biocondaLicense := (licenses in Bioconda).value.toList.headOption.getOrElse("No license","")._1,
    biocondaTestCommands := Seq()
  )

  override def globalSettings: Seq[Def.Setting[_]] = Def.settings(
    )

  private def initBiocondaRepo: Def.Initialize[Task[File]] = {
    Def.task {
      val initialized: Boolean =
        new File(biocondaRepository.value, ".git").exists()
      val git = GitKeys.gitRunner.value
      val s = streams.value
      val local = biocondaRepository.value
      val upstream = biocondaMainGitUrl.value
      val origin = biocondaMainGitUrl.value
      val branch = biocondaMainBranch.value

      //Check if git repo already exits
      if (!initialized) {
        git.apply("init")(local, s.log)
      }

      // Check if remotes have been set properly
      val remotes: Array[String] =
        git.apply("remote")(local, s.log).split("\\n")

      if (remotes.contains("upstream")) {
        git.apply("remote", "set-url", "upstream", upstream)(local, s.log)
      } else {
        git.apply("remote", "add", "upstream", upstream)(local, s.log)
      }
      if (remotes.contains("origin")) {
        git.apply("remote", "set-url", "origin", origin)(local, s.log)
      } else {
        git.apply("remote", "add", "origin", origin)(local, s.log)
      }

      // Check if biocondaMainBranch exists. If not create it.

      if (branchExists(branch, local, git, s.log)) {
        git.apply("checkout", branch)(local, s.log)
      } else {
        git.apply("checkout", "-b", branch)(local, s.log)
      }

      // Get latest recipes from main repository.
      git.apply("pull", "upstream", branch)(local, s.log)
      local
    }
  }

  private def updateBranch(): Def.Initialize[Task[File]] = {
    Def.task {
      val git = GitKeys.gitRunner.value
      val s = streams.value
      val local: File = biocondaRepository.value
      val branch: String = biocondaBranch.value
      val mainBranch: String = biocondaMainBranch.value

      // Check if tool branch exists, create it otherwise.
      if (branchExists(branch, local, git, s.log)) {
        git.apply("checkout", branch)(local, s.log)
      } else {
        git.apply("checkout", "-b", branch)(local, s.log)
      }
      // Rebase tool branch on main branch
      git.apply("rebase", mainBranch)(local, s.log)
      local
    }
  }

  private def getSha256SumFromDownload(url: String): String = {
    import sys.process._
    val jar = new URL(url)
    val tmp = java.io.File.createTempFile("bioconda",".jar")
    tmp.deleteOnExit()
    val download = jar #> tmp
    download.run()
    tmp.sha256.hex
  }



  private def getSourceUrl(tag: TagName, repo: GHRepository): Option[String] = {
    val releaseList = repo.listReleases().asList()
    val releases =
      JavaConverters.collectionAsScalaIterable(releaseList).toList
    val currentRelease = releases.find(x => x.getTagName == tag)
    if (currentRelease.isEmpty) {
      throw new Exception(
        s"'$tag' tag not present on release page. Please release on github before publishing to bioconda.")
    }
    val assets = JavaConverters
      .collectionAsScalaIterable(
        currentRelease.getOrElse(new GHRelease).getAssets)
      .toList
    val releaseJar =
    // Finds all jars. This assumes only one jar is released.
      assets.find(x => x.getBrowserDownloadUrl.contains(".jar"))
    if (releaseJar.isEmpty) {None}
    else Some(releaseJar.getOrElse(new GHAsset).getBrowserDownloadUrl)
  }
  private def createVersionRecipes: Def.Initialize[Task[File]] = Def.task {
    val publishedTags: Seq[TagName] = getPublishedTags.value
    val releasedTags: Seq[TagName] = getReleasedTags.value
    // Tags that are released but not in bioconda yet should be published
    val toBePublishedTags = releasedTags.filter(tag => !publishedTags.contains(tag))
    val repo = ghreleaseGetRepo.value
    val log = streams.value.log
    // Some sbt magic here. We initialize a task that returns the recipe dir.
    // We add dependencies to this task based on the tags
    for (tag <- toBePublishedTags) {
      // Hardcoded "v".
      val versionNumber = tag.stripPrefix("v")
      val publishDir = new File(biocondaRecipeDir.value, versionNumber)
      val sourceUrl = getSourceUrl(tag,repo)
      if (sourceUrl.isEmpty) {
        log.error(s"No released jar for tag: $tag. Skipping.")
      }
      else {
        val recipe = new BiocondaRecipe(
          name = (name in Bioconda).value,
          //Hardcoded "v" prefix here.
          version = versionNumber,
          sourceUrl = sourceUrl.get,
          sourceSha256 = getSha256SumFromDownload(sourceUrl.get),
          runRequirements = biocondaRequirements.value,
          homeUrl = (homepage in Bioconda).value.getOrElse("").toString,
          license = biocondaLicense.value,
          buildRequirements = biocondaBuildRequirements.value,
          summary = biocondaSummary.value,
          buildNumber = biocondaBuildNumber.value,
          notes = Some(biocondaNotes.value),
          defaultJavaOptions = biocondaDefaultJavaOptions.value,
          testCommands = biocondaTestCommands.value
        )
        publishDir.mkdirs()
        recipe.createRecipeFiles(publishDir)
      }
    }
    biocondaRecipeDir.value
  }.dependsOn(biocondaUpdatedBranch)

  private def createLatestRecipes: Def.Initialize[Task[File]] = Def.task {
    val releasedTags: Seq[TagName] = getReleasedTags.value
    val tag = releasedTags.sortBy(tag => tag.stripPrefix("v")).headOption.getOrElse("No version")
    val repo = ghreleaseGetRepo.value
    val log = streams.value.log
    val versionNumber = tag.stripPrefix("v")
    val publishDir = biocondaRecipeDir.value
    val sourceUrl = getSourceUrl(tag,repo)
    if (sourceUrl.isEmpty) {
      log.error(s"No released jar for tag: $tag. Skipping.")
    }
    else {
      val recipe = new BiocondaRecipe(
        name = (name in Bioconda).value,
        //Hardcoded "v" prefix here.
        version = versionNumber,
        sourceUrl = sourceUrl.get,
        sourceSha256 = getSha256SumFromDownload(sourceUrl.get),
        runRequirements = biocondaRequirements.value,
        homeUrl = (homepage in Bioconda).value.getOrElse("").toString,
        license = biocondaLicense.value,
        buildRequirements = biocondaBuildRequirements.value,
        summary = biocondaSummary.value,
        buildNumber = biocondaBuildNumber.value,
        notes = Some(biocondaNotes.value),
        defaultJavaOptions = biocondaDefaultJavaOptions.value,
        testCommands = biocondaTestCommands.value
      )
      publishDir.mkdirs()
      recipe.createRecipeFiles(publishDir)
    }
    biocondaRecipeDir.value
  }.dependsOn(biocondaUpdatedBranch)

  def branchExists(branch: String,
                   repo: File,
                   git: GitRunner,
                   log: ManagedLogger): Boolean = {
    // TODO: Find a git command that just returns branches as a list. (Without * in front of the branch you are on)

    // Without "--no-color" scala doesn't match the strings properly. Color matters in string comparison!
    val branchList: Array[String] =
      git.apply("branch", "-a", "--no-color")(repo, log).split("\\n")

    val branches = new ListBuffer[String]

    // For each branch
    // Remove that annonoying *
    // Split on / and get the last item(remotes/origin/branch) => branch
    // Trim away all spaces
    branchList.foreach(x =>
      branches.append(x.replaceFirst("\\*", "").split("/").last.trim()))
    branches.toList.contains(branch)

  }
  private def defaultSummary: Def.Initialize[String] =
    Def.setting {
      s"""This summary for ${(name in Bioconda).value} is automatically generated.
         |Please visit ${(homepage in Bioconda).value} for more information about this program.
       """.stripMargin
    }

  private def defaultNotes: Def.Initialize[String] =
    Def.setting {
      def javaOpts: String = {
        val javaDefaults = biocondaDefaultJavaOptions.value
        val builder = new StringBuilder
        javaDefaults.foreach(x => builder.append(x + " "))
        builder.toString().trim
      }
      s"""${(name in Bioconda).value} is Java program that comes with a custom wrapper shell script.
         |By default “$javaOpts” is set in the wrapper.
         |If you want to overwrite it you can specify memory options directly after your binaries.
         |If you have _JAVA_OPTIONS set globally this will take precedence.
         |For example run it with “${(name in Bioconda).value} -Xms512m -Xmx1g”
         |
       """.stripMargin
    }

  def getVersionFromYaml(metaYaml: File): String = {
    def yaml = new Yaml(new Constructor(classOf[BiocondaMetaYaml]))

    val yamlFile = new FileInputStream(metaYaml)
    val meta: BiocondaMetaYaml = yaml.load(yamlFile)
    meta.package_info.version
  }

  private def getPublishedTags: Def.Initialize[Task[Seq[TagName]]] = {

    def crawlRecipe(recipe: File): Seq[File] = {
      val files = recipe.listFiles()
      val yamls = new ArrayBuffer[File]()
      for (file <- files){
        if (file.isDirectory) {
          yamls ++= crawlRecipe(file)
        }
        if (file.base == "meta.yaml"){
          yamls.append(file)
        }
      }
      yamls
    }

    Def
      .task {
        val recipes: File = new File(biocondaRepository.value, "recipes")
        val thisRecipe: File = new File(recipes, (name in Bioconda).value)

        def tags = new ArrayBuffer[String]

        if (thisRecipe.exists()) {
          val metaYamls = crawlRecipe(thisRecipe)
          // Hardcoded "v" prefix here. Is the standard in github release plugin.
          // But not a very nice way of doing it.
          metaYamls.foreach(x => tags.append("v" + getVersionFromYaml(x)))
        }
          tags.toSeq.distinct
      }
      .dependsOn(biocondaUpdatedBranch)
  }

  private def getReleasedTags: Def.Initialize[Task[Seq[TagName]]] = {
    Def.task {
      val repo = ghreleaseGetRepo.value
      val releaseList = repo.listReleases().asList()
      val releases =
        JavaConverters.collectionAsScalaIterable(releaseList).toList
      val tags = new ArrayBuffer[TagName]()
      releases.foreach(x => tags.append(x.getTagName))
      if (tags.isEmpty) {
        throw new Exception("No tags have been released. Please release on github before publishing to bioconda")
      }
      tags
    }
  }
}
