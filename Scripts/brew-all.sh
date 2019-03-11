#!/usr/bin/env bash

 # Check for missing brew
if [ -z `command -v brew` ]; then
	echo "Brew is missing! Installing it..."
	ruby -e "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/master/install)"
fi;

# Make sure we’re using the latest Homebrew
brew update

# Upgrade any already-installed formulae
brew upgrade

# Install GNU core utilities (those that come with OS X are outdated)
# Don’t forget to add `$(brew --prefix coreutils)/libexec/gnubin` to `$PATH`.
brew install coreutils
brew install moreutils
brew install findutils

# Install GNU `sed`, overwriting the built-in `sed`
brew install gnu-sed --default-names

# Install Bash 4
# Note: don’t forget to add `/usr/local/bin/bash` to `/etc/shells` before running `chsh`.
brew install bash
brew install bash-completion


# Install wget with IRI support
brew install wget --enable-iri

brew install maven
brew install python3
brew install tree

brew install aws
brew install terraform
brew install terragrunt

brew cleanup

brew install httpie
