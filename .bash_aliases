# ------------------------------------------------------------------------------
#
# .bash_aliases
#
# ------------------------------------------------------------------------------


# ------------------------------------------------------------------------------
#
#                                   Aliases
#
# ------------------------------------------------------------------------------

# cp with prompt if overwriting.
#
alias cpi='cp -i'

# Default df and du to human readable figures.
#
alias dfh='df -h'
alias duh='du -h'

if [ -x /usr/bin/dircolors ]; then
  test -r ~/.dircolors && eval "$(dircolors -b ~/.dircolors)" || eval "$(dircolors -b)"

  # Grep highlighting matches in color.
  #
  alias egrepc='egrep --color'
  alias fgrepc='fgrep --color'
  alias grepc='grep --color'

  # Long listing.
  #
  alias ll='lsc -l'

  # Color alias for ls.
  #
  alias lsc='ls --color=always'
fi

# mv with prompt.
#
alias mvi='mv -i'

# rm with prompt as default.
#
alias rm='rm -i'

# tail a file with follow
#
alias tf='tail -f'

# tail a file and follow starting at the end
#
alias tft='tail -f -n 0'

# ------------------------------------------------------------------------------
#
#                                  Functions
#
# ------------------------------------------------------------------------------

# Grep History
#
hgrep() { 
  history | grep $1; 
}

hgrepc() {
  history | grep --color $1;
}

# Make new directory, then change to it.
#
mkcd() { 
  mkdir -p "$1" && cd "$1"; 
}


# Uses scp to copy a file to a named server, then ssh to exec chmod to
# grant group rw permissions.
#
# Arguments:
# $1  the absolute path to the local file.
# $2  server name (can use shortcut names in ~/.ssh/config).
# $3  the absolute path to the file on the target server (name included).
#
scp_grw() {
  if [ $# -ne 3 ]; then
     echo "Usage: $FUNCNAME PATH_TO_FILE SERVER_NAME SERVER_PATH"
     return 1
  fi
  
  local fname=$(basename $1)
  scp $1 $2:$3/$fname
  ssh $2 chmod g+rw $3/$fname
}


# Launch emacs client without waiting for a return.
#
emacsc() { 
  $EMACS_CLIENT -c -n "$*";
  if (( $? )); then
    $EMACS --daemon
    $EMACS_CLIENT -c -n "$*";
  fi
}

# Note: if pgrep is not present, this works on ubunutu:
# ps -C emacs24 -o pid=
emacsk() {
  if (( $(pgrep emacs) )); then
    $EMACS_CLIENT -e "(kill-emacs)"
  fi
}


# ------------------------------------------------------------------------------
#
#                  Source convenience functions by "category"
#
# ------------------------------------------------------------------------------

if [ -f ~/Scripts/svn_functions ]; then
  . ~/Scripts/svn_functions
fi

if [ -f ~/Scripts/maven_functions ]; then
  . ~/Scripts/maven_functions
fi

if [ -f ~/Scripts/cernunnos_functions ]; then
  . ~/Scripts/cernunnos_functions
fi

if [ -f ~/Scripts/cvs_functions ]; then
  . ~/Scripts/cvs_functions
fi

if [ -f ~/Scripts/project_functions ]; then
  . ~/Scripts/project_functions
fi
