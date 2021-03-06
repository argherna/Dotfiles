#!/bin/bash

# ------------------------------------------------------------------------------
#
# mvn_functions
#
# Contains convenience functions for using Maven. These functions rely on the
# pre-installed version of Maven that comes with Mac OS X.
#
# ------------------------------------------------------------------------------


# ------------------------------------------------------------------------------
#
#                            Environment variables
#
# ------------------------------------------------------------------------------

# No environment variables set here (rely on system to set JAVA_HOME)

# ------------------------------------------------------------------------------
#
#                                  Functions
#
# ------------------------------------------------------------------------------

# Run mvn clean. Arguments are optional but will be processed BEFORE the clean 
# goal is run.
#
mvn_cl() {
  if [ $# -eq 0 ]; then
    mvn clean
  else
    mvn "$*" clean
  fi
}

alias mvnc='mvn_cl'

# Run mvn clean install. Arguments are optional but will be processed BEFORE
# the clean and install goals are run.
#
mvn_ci() {
  if [ $# -eq 0 ]; then
    mvn clean install
  else
    mvn $* clean install
  fi
}

alias mvnci='mvn_ci'


# Run mvn clean package. Arguments are optional but will be processed BEFORE
# the clean and package goals are run.
#
mvn_cp() {
  if [ $# -eq 0 ]; then
    mvn clean package
  else
    mvn $* clean package
  fi
}

alias mvncp='mvn_cp'

# Installs the named jar file with the specified version number in the local maven
# repository (usu. in $HOME/.m2/repository). The artifactId and groupId can be 
# specified too, but the basename of the jar file is assumed if they are not.
#
# Arguments:
#
# $1    path to the jar file to install (required).
#
# $2    the version number (required).
#
# $3    the artifact ID (optional).
#
# $4    the group ID (optional).
#
mvn_inj() {
  if [ $# -lt 2 ]; then
    echo "Usage: $FUNCNAME JAR_FILE VERSION [ARTIFACT_ID] [GROUP_ID]"
    echo "Note: basename of JAR_FILE is assumed if ARTIFACT_ID or GROUP_ID are omitted"
    return 1
  fi
  
  local jar_file=$1
  local version=$2

  local artifact_id=$(basename $jar_file)
  if [ $# -gt 2 ]; then
    artifact_id=$3
  fi

  local group_id=$(basename $jar_file)
  if [ $# -gt 3 ]; then
    group_id=$4
  fi

  mvn install:install-file                      \
    -Dfile=$jar_file                            \
    -Dversion=$version                          \
    -DartifactId=$artifact_id                   \
    -DgroupId=$group_id                         \
    -Dpackaging=jar
}


# Generates a new pom project module. Has to be run in the directory you want your
# project to be created in. If this is a submodule to a parent, set the group Id
# parameter to [PARENT_GROUP_ID].[THIS_GROUP_ID]. 
#
# Arguments:
#
# $1    group Id
#
# $2    artifact Id
#
# $3    package name
#
mvn_gennewpom() {
  if [ $# -lt 3 ]; then
    echo "Usage: $FUNCNAME GROUP_ID ARTIFACT_ID PKG"
    return 1
  fi
    
  local parent_group_id=$1
  local parent_artifact_id=$2
  local parent_pkg=$3

  mvn archetype:generate                                \
    -DarchetypeGroupId=org.codehaus.mojo.archetypes     \
    -DarchetypeArtifactId=pom-root                      \
    -DgroupId=$parent_group_id                          \
    -DartifactId=$parent_artifact_id                    \
    -DpackageName=$parent_pkg                           \
    -Dpackaging=pom                                     \
    -Dversion=1.0.0
}


# Generates a new web project module. Has to be run in the directory you want your
# project to be created in. If this is a submodule to a parent, set the group Id
# parameter to [PARENT_GROUP_ID].[THIS_GROUP_ID]. 
#
# Arguments:
#
# $1    group Id
#
# $2    artifact Id
#
# $3    package name
#
mvn_gennewweb() {
  if [ $# -lt 3 ]; then
    echo "Usage: $FUNCNAME GROUP_ID ARTIFACT_ID PKG"
    return 1
  fi
    
  local group_id=$1
  local artifact_id=$2
  local pkg=$3
                                                        \
  mvn archetype:generate                                \
    -DarchetypeGroupId=org.apache.maven.archetypes      \
    -DarchetypeArtifactId=maven-archetype-webapp        \
    -DgroupId=$group_id                                 \
    -DartifactId=$artifact_id                           \
    -DpackageName=$pkg                                  \
    -Dversion=1.0.0
}


# Generates a new web project module. Has to be run in the directory you want your
# project to be created in. If this is a submodule to a parent, set the group Id
# parameter to [PARENT_GROUP_ID].[THIS_GROUP_ID]. 
#
# Arguments:
#
# $1    group Id
#
# $2    artifact Id
#
# $3    package name
#
mvn_gennewjar() {
  if [ $# -lt 3 ]; then
    echo "Usage: $FUNCNAME GROUP_ID ARTIFACT_ID PKG"
    return 1
  fi
    
  local group_id=$1
  local artifact_id=$2
  local pkg=$3

  mvn archetype:generate                                \
    -DarchetypeGroupId=org.apache.maven.archetypes      \
    -DarchetypeArtifactId=maven-archetype-quickstart    \
    -DgroupId=$group_id                                 \
    -DartifactId=$artifact_id                           \
    -DpackageName=$pkg                                  \
    -Dversion=1.0.0
}
