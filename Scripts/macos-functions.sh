#!/bin/bash

# ------------------------------------------------------------------------------
#
# macos-functions.sh
#
# Contains convenience functions specific to Mac OSX.
#
# ------------------------------------------------------------------------------

# ------------------------------------------------------------------------------
#
#                            Environment Variables
#
# ------------------------------------------------------------------------------


# Colorize directory listings (2 colors/attribute):
#
#   directory                                       bold blue
#   symbolic link                                   bold cyan
#   socket                                          green
#   pipe                                            brown (yellow)
#   executable                                      bold green
#   block special                                   blue on cyan bg
#   character special                               blue on brown (yellow) bg
#   executable with setuid bit set                  black on red bg
#   executable with setgid bit set                  black on cyan bg
#   directory writable to others, w/ sticky bit     black on green bg
#   directory writable to others, w/out sticky bit  black on brown (yellow) bg
#
# Color codes for Mac OSX:
#
#   a  black
#   b  red
#   c  green
#   d  brown
#   e  blue
#   f  magenta
#   g  cyan
#   h  light grey
#   A  bold black, usually shows up as dark grey
#   B  bold red
#   C  bold green
#   D  bold brown, usually shows up as yellow
#   E  bold blue
#   F  bold magenta
#   G  bold cyan
#   H  bold light grey; looks like bright white
#   x  default foreground or background
#
export CLICOLOR=1
export LSCOLORS=ExGxcxdxCxegedabagacad 

# DbVisualizer
#
export DBVIS_CLI_HOME=/Applications/DbVisualizer.app/Contents/java/app


# ------------------------------------------------------------------------------
#
#                                  Functions
#
# ------------------------------------------------------------------------------


# Launch the DbVisualizer command line tool.
#
dbviscmd() {
  local dbvis_cmd=$DBVIS_CLI_HOME/dbviscmd.sh 
  $dbvis_cmd "$*"
}

# Get rid of DS_STORE (Mac OSX only)
#
un_dsstore() {
  local DSSTORE_DIR=${1:-$(pwd)}
  find $DSSTORE_DIR -type f -name ".DS_Store" -exec rm -rf {} \;
}

un_macosx() {
  local MACOSX_DIR=${1:-$(pwd)}
  find $MACOSX_DIR -type d -name "__MACOSX" -exec rm -rf {} \;
}

# Converts a text file with '\r\n' line endings (DOS files) to '\n' line 
# endings (Unix files). Files are copied to their original file names with a
# '.unix' suffix.
#
# Arguments:
#
# $*    Absolute paths to files to convert.
#
d2u() {
  if [ $# - eq 0 ]; then
     echo "Usage: $FUNCNAME <dos filename> [<dos filename> ...]"
    return 1
  fi
    
  for DOS_FILE in $*; do
    tr -d '\r' < $DOS_FILE > ${DOS_FILE}.unix
  done
}

# copies the named file to the system clipboard
#
copyfile() {
  if [ $# -eq 0 ]; then
    echo "Usage: $FUNCNAME <filename>"
    return 1
  fi
  local file_to_copy=$1
  pbcopy < $file_to_copy
}

# Generates a UUID and copies it to the pasteboard.
#
cp_uuid() {
  uuidgen | pbcopy
}

# ------------------------------------------------------------------------------
#
#                                   Aliases
#
# ------------------------------------------------------------------------------


# Grep highlighting matches in color.
#
alias grepc='grep --color'

# Long listing.
#
alias ll='lsc -l'

# Color alias for ls.
#
alias lsc='ls -G'

# Set up Emacs (Mac OS X version from <URL:http://emacsformacosx.com> to
# start in the terminal window.
#
alias emacsnw='/Applications/Emacs.app/Contents/MacOS/Emacs -nw'

# Open OS X apps from the command line.
#
alias Adium='open /Applications/Adium.app/'
alias Calculator='open /Applications/Calculator.app/'
alias Calendar='open /Applications/Calendar.app/'
alias Chrome='open /Applications/Google\ Chrome.app/'
alias Console='open /Applications/Utilities/Console.app/'
alias DbViz='open /Applications/DbVisualizer.app/'
alias Dictionary='open /Applications/Dictionary.app/'
alias Eclipse='open /Applications/eclipse\ kepler/Eclipse.app/'
alias Emacs='open /Applications/Emacs.app/'
alias Firefox='open /Applications/Firefox.app/'
alias iTunes='open /Applications/iTunes.app/'
alias KeePassX='open /Applications/KeePassX.app/'
alias Lync='open /Applications/Microsoft\ Lync.app/'
alias Mail='open /Applications/Mail.app/'
alias Preview='open /Applications/Preview.app/'
alias Safari='open /Applications/Safari.app/'
alias VisualVm='open /Applications/VisualVm.app/'
alias Vlc='open /Applications/VLC.app/'

# Open specific Eclipse workspaces.
#
alias Cis='open -n /Applications/eclipse/Eclipse.app/ --args -data ~/Documents/workspace/StudentServices/CourseInformationSuite/Code'
alias Cloudbroker='open -n /Applications/eclipse/Eclipse.app/ --args -data ~/Documents/workspace/CloudBroker/Code/cloud-broker'
alias Commons='open -n /Applications/eclipse/Eclipse.app/ --args -data ~/Documents/workspace/Commons/Code'
alias LinkAgg='open -n /Applications/eclipse/Eclipse.app/ --args -data ~/Documents/workspace/StudentServices/LinkAggregator/Code'
alias LogicLander='open -n /Applications/eclipse/Eclipse.app/ --args -data ~/Documents/workspace/LogicLander/Code'
alias NetworkTools='open -n /Applications/eclipse/Eclipse.app/ --args -data ~/Documents/workspace/NetworkTools/Code'
alias Lwtc='open -n /Applications/eclipse/Eclipse.app/ --args -data ~/Documents/workspace/Lwtc/Code/Java'
alias Maven='open -n /Applications/eclipse/Eclipse.app/ --args -data ~/Documents/workspace/Maven/Code/Java'



if [[ -x $(which brew 2>/dev/null) ]]; then
    if [ -f $(brew --prefix)/etc/bash_completion ]; then
        . $(brew --prefix)/etc/bash_completion
    fi
fi
