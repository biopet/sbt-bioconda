# SBT-BIOCONDA
This plugin publishes scala or java tools on bioconda. 
It can be used to publish to other conda repositories as well. 
It automatically creates a recipe with a wrapper script. The wrapper script
allows the command to be run with `application` instead of `java -jar application.jar`. 

The recipes created are in concordance with [bioconda's guidelines](https://bioconda.github.io/guidelines.html#guidelines).

## Installation

put
```scala
AddSbtPlugin("com.github.biopet","sbt-bioconda","0.1")
```
in your `project/plugins.sbt` file.
### requirements:
- the [Sbt Github release plugin](https://github.com/ohnosequences/sbt-github-release)
must be used in your project. The repo authentication from
this plugin is used. Also the bioconda plugin works very well in combination with this plugin.
- the [sbt git plugin]() must be used in your project.
This plugin is used to clone the bioconda repository.
- Docker must be installed on the system. And your user should be in the
`docker` group on your system. (In other words: be able to run docker without `sudo`).
Docker is used to test the recipes you create.


## Usage
The following settingKeys are compulsory
```scala
biocondaGitUrl := "https://github.com/yourorganisation/bioconda-recipes" // your personal fork of bioconda-recipes
```
The following settingKeys can be used to tune the bioconda recipes to your liking:
```scala
name in Bioconda := "my_program" // Name of your tool on bioconda. Defaults to normalizedName
biocondaCommand := "my-program" // the command on the shell to execute your program. Defaults to name in Bioconda
biocondaSummary := "description ..." // The summary of your program. Defaults to a generic description with a link to your homepage.
homepage in Bioconda := "example.com" // The homepage of your project as displayed on bioconda. Defaults to homepage of your project.
biocondaRequirements := Seq("openjdk") // The requirements for your project. Defaults to Seq("openjdk")
biocondaBuildRequirements := Seq() // The build requirements for your project. Defaults to Seq()
biocondaTestCommands := Seq("my-program --version", "my-program --test") // The commands that are used to test your recipe. Defaults to Seq()
biocondaDefaultJavaOptions: Seq() // You can use this to set the default memory options. Default is empty.
biocondaOverwriteRecipes := false // You can use this if you want to overwrite previously published recipes. (Default: false)
biocondaPullRequestTitle := "my_program" // The title of the pull request on github.
biocondaPullRequestBody := " ... " // The pull request body. Default is a text on the automated process of sbt-bioconda and a summary of the tool. Taken from biocondaSummary.
biocondaLicense := "MIT" // The license you use. By default checks your licenses key and takes one. Better to define this explicitly.
```
The following settingKeys can be used for more advanced settings. 
The defaults are designed for bioconda. 
```scala
biocondaMainGitUrl := "https://github.com/bioconda/bioconda-recipes.git" // conda repository to which you want to pull.
biocondaMainBranch := "master" // The branch on which the stable recipes on the conda repository are defined.
biocondaNotes := " ... " // Notes on how to run the program this is compulsory for bioconda. Default message explains the wrapper.
biocondaBuildNumber := 0 // The build number. This defaults to zero. Change this if you published the same version twice. (This should never happen!)
```

The following settingKeys are for internal operation. You can change these at will.
```scala
biocondaBranch := "myprogram" // the branch on which your pull request is created. Defaults to normalizedName in Bioconda
biocondaRepository := "/home/user/bioconda-recipes" // Where bioconda repository is located. Will be created if not existing. Defaults to target.value/bioconda. 
biocondaRecipeDir := "recipes" // Where the recipes will be created. Defaults to target.value/recipes
biocondaCommitMessage := "Auto" // The default commit message. Default is "automated update for recipes of (name in Bioconda).value. 
```

Task keys:
- `biocondaUpdatedRepository` makes sure a bioconda repository is present and up to date
with bioconda main.
- `biocondaUpdatedBranch` creates a branch for your tool that is up to date with bioconda main.
- `biocondaCreateRecipes` creates the recipes for your tool. It checks which are already in bioconda main and only adds new ones. Unless biocondaOverwriteRecipes == true
- `biocondaAddRecipes` adds the recipes to the local repository.
- `biocondaTestRecipes` tests the newly added recipes.
- `biocondaPushRecipes` pushes the local tool branch to your fork of bioconda.
- `biocondaPullRequest` create a new pull request on bioconda main.
- `biocondaRelease` does all of the above. You can add this command to your release procedure.

If you do not want to publish all the recipes add once (default for biocondaRelease) you can 
run the above commands manually and use the following keys:
- `biocondaCreateVersionRecipes` creates all the recipe versions, but does not change the latest versions.
- `biocondaCreateLatestRecipe` only updates the latest released version.

## Known Issues

- Versions with a `-` in them crash on testing. This is a bioconda requirement.
If you released versions with a - in them this will crash your `biocondaTestRecipes` command 
and therefore also `biocondaRelease`.
