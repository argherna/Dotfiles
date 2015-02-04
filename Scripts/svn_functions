#!/bin/bash

# ------------------------------------------------------------------------------
#
# svn_functions
#
# Contains convenience functions for using SVN.
#
# ------------------------------------------------------------------------------


# ------------------------------------------------------------------------------
#
#                            Environment variables
#
# ------------------------------------------------------------------------------

# Use a custom-installed version of subversion. Set it before the PATH.
#
if [ -d /opt/subversion/bin ]; then
  export PATH=/opt/subversion/bin:$PATH
fi


# ------------------------------------------------------------------------------
#
#                                  Functions
#
# ------------------------------------------------------------------------------

# Deletes the .svn directories from a named directory. If no directory is
# specified, the current directory is assumed.
#
# Arguments:
# 
# $1   The directory to delete .svn directories from (optional)
#
un_svn() {
  local svndir=$(pwd)
  if [ $# -eq 0 ]; then
    svndir=$1
  fi

  find $svndir -type d -name ".svn" -exec rm -rf {} \;
}


# Rolls back the specified file or URL to the given target revision. A source
# revision can be given but if it isn't, HEAD is assumed.
#
# WARNING: this function performs a commit after the merge. Be sure you want to
# use this function before using it. If you are unsure about what this function
# will do, run svn_rollback_preview first.
#
# Arguments:
#
# $1    The file, directory or URL to roll back (required).
#
# $2    The target revision number to roll back to (required).
#
# $3    The source revision. If not specified, HEAD is assumed (optional).
#
svn_rollback() {
  if [ $# -lt 2 ]; then
    echo "Usage: $FUNCNAME FILE_OR_URL TARGET_REVISION [SOURCE_REVISION]"
    echo "If SOURCE_REVISION is not specified, HEAD is assumed."
    return 1
  fi
  
  local rev="HEAD"
  if [ $# -eq 3 ]; then
    rev=$3
  fi
  
  svn merge -r${rev}:$2 $1
  svn commit -m "Rollback $1 from revision ${rev} to revision $2" $1
}


# Preview a rollback as it is executed by the svn_rollback function. A source
# revision can be given but if it isn't, HEAD is assumed.
#
# Arguments:
#
# $1    The file, directory or URL to roll back (required).
#
# $2    The target revision number to roll back to (required).
#
# $3    The source revision. If not specified, HEAD is assumed (optional).
#
svn_rollback_preview() {
  if [ $# -lt 2 ]; then
    echo "Usage: $FUNCNAME FILE_OR_URL TARGET_REVISION [SOURCE_REVISION]"
    echo "If SOURCE_REVISION is not specified, HEAD is assumed."
    return 1
  fi
  
  local rev="HEAD"
  if [ $# -eq 3 ]; then
     rev=$3
  fi
  
  svn merge --dry-run -r${rev}:$2 $1
}


# Sets the svn:ignore property on the specified local file.
#
# Arguments:
#
# $1    The file to set the svn:ignore property on.
#
svn_ignorefile() {
  if [ $# -eq 0 ]; then
    echo "Usage: $FUNCNAME LOCAL_FILE"
    return 1
  fi
  
  svn propset svn:ignore -F $1 .
}


# Sets the svn:ignore property on the specified local directory.
#
# Arguments:
#
# $1    The directory to set the svn:ignore property on.
#
svn_ignoredir() {
  if [ $# -eq 0 ]; then
    echo "Usage: $FUNCNAME DIRECTORY_NAME"
    return 1
  fi
  
  svn propset svn:ignore $1 .
}