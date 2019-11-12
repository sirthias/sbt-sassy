# SBT plugin for Dart SASS/SCSS Compiler

[![Version](https://api.bintray.com/packages/bullet/sbt-plugins/sbt-sassy/images/download.svg)](https://bintray.com/bullet/sbt-plugins/sbt-sassy/_latestVersion)

An SBT-plugin that enables compilation of [SASS](http://sass-lang.com/) sources in your project.

This plugin is based on [sbt-sass by @madoushi](https://github.com/madoushi/sbt-sass), which itself is based on 
[sbt-sass by @ShaggyYeti](https://github.com/ShaggyYeti/sbt-sass), which is based on
[play-sass](https://github.com/jlitola/play-sass).
All three previous plugin versions appear to be abandoned and do not support the current
[Dart Sass] compiler, which is the "leading" one at the time of this writing. 


## Prerequisites

[Dart Sass] needs to be installed for the plugin to work.
On macOS [Dart Sass] is available via [Homebrew](https://brew.sh/) and can be installed with:

    % brew install sass/sass/sass   


## Installation

Include the plugin in `project/plugins.sbt` as such:

    addSbtPlugin("io.bullet" % "sbt-sassy" % "<VERSION>") 


## Compatibility

The plugin is based on [sbt-web] and should be compatible with SBT 0.13 and 1.x.


## Usage

_sbt-sassy_ follows the conventions of [sbt-web] regarding [directory layout](https://github.com/sbt/sbt-web#file-directory-layout).
By default the plugin looks for `*.sass` and `*.scss` sources underneath the `src/main/assets` folder,
so `src/main/assets/sass` would be a good place to store your files.

Files starting with an underscore (`_`) are not compiled into CSS files.
They can, however, be referenced from other SASS/SCSS files via an `@import` directive.


### Options

_sbt-sassy_ can be configured via a few SBT settings.
See the [sources](https://github.com/sirthias/sby-sassy/blob/master/src/main/scala/io/bullet/sbt/sass/SbtSass.scala#L21) for more info. 


Patches and contributions are always welcome!


  [Dart Sass]: https://sass-lang.com/dart-sass
  [sbt-web]: https://github.com/sbt/sbt-web