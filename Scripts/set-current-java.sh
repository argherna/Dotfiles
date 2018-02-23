#!/bin/bash

# ------------------------------------------------------------------------------
#
# set_current_java
#
# Creates a symlink to the specified version of the JDK. The symlink is 
# in $HOME/java and it's named 'current'. Note: $HOME/java/current is set on the
# PATH for this user.
#
# Arguments:
#   VERSION - the version of the JDK to make current.
#
# ------------------------------------------------------------------------------

if [ $# -eq 0 ]; then
    cat<<EOF
Usage: $(basename $0) VERSION
EOF
fi

if [ -d $HOME/java/jdk$1 ]; then

  if [ -L $HOME/java/current ]; then
    rm -f $HOME/java/current
  fi

  ln -s $HOME/java/jdk$1 $HOME/java/current
  return 0

else
  echo "Version $1 not found."
  return 1
fi