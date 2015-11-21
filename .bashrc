# ------------------------------------------------------------------------------
#
# ~/.bashrc 
#
# executed by bash(1) for non-login shells.
# 
# ------------------------------------------------------------------------------

# If not running interactively, don't do anything
#
[ -z "$PS1" ] && return


# ------------------------------------------------------------------------------
#
#                         Source important stuff first
#
# ------------------------------------------------------------------------------

if [ -f ~/.bash/.bash_preinit ]; then
  . ~/.bash/.bash_preinit
fi


# ------------------------------------------------------------------------------
#
#                            Environment Variables
#
# ------------------------------------------------------------------------------

# Set History size to be large. See:
# <URL:http://www.oreillynet.com/onlamp/blog/2007/01/whats_in_your_bash_history.html>
#
export HISTFILESIZE=100000000
export HISTSIZE=100000
export HISTCONTROL=ignoredups:ignorespace
export EDITOR=$EMACS_CLIENT

# ------------------------------------------------------------------------------
#
#                              Set Shell Options
#
# ------------------------------------------------------------------------------
shopt -s checkwinsize
shopt -s histappend

# make less more friendly for non-text input files, see lesspipe(1)
#
[ -x /usr/bin/lesspipe ] && eval "$(SHELL=/bin/sh lesspipe)"

# set variable identifying the chroot you work in (used in the prompt below)
#
if [ -z "$debian_chroot" ] && [ -r /etc/debian_chroot ]; then
    debian_chroot=$(cat /etc/debian_chroot)
fi

# ------------------------------------------------------------------------------
#
#                                   Prompts
#
# ------------------------------------------------------------------------------

# set a fancy prompt (non-color, unless we know we "want" color)
#
case "$TERM" in
    xterm-color) color_prompt=yes;;
esac

# Define colors.
#
GREY=$'\033[1;30m'
RED=$'\033[1;31m'
GREEN=$'\033[1;32m'
YELLOW=$'\033[1;33m'
BLUE=$'\033[1;34m'
MAGENTA=$'\033[1;35m'
CYAN=$'\033[1;36m'
WHITE=$'\033[1;37m'
NONE=$'\033[m'

# uncomment for a colored prompt, if the terminal has the capability; turned
# off by default to not distract the user: the focus in a terminal window
# should be on the output of commands, not on the prompt
#
#force_color_prompt=yes

if [ -n "$force_color_prompt" ]; then
    if [ -x /usr/bin/tput ] && tput setaf 1 >&/dev/null; then
	# We have color support; assume it's compliant with Ecma-48
	# (ISO/IEC-6429). (Lack of such support is extremely rare, and such
	# a case would tend to support setf rather than setaf.)
	color_prompt=yes
    else
	color_prompt=
    fi
fi

# parse_git_branch() {
#   git branch 2> /dev/null | sed -e '/^[^*]/d' -e 's/* (.*)/(1)/'
# }

# export PS1="[33[00m]u@h[33[01;34m] W [33[31m]$(parse_git_branch) [33[00m]$[33[00m] "
 

if [ "$color_prompt" = yes ]; then
    PS1='${debian_chroot:+($debian_chroot)}\[\033[01;32m\]\u@\h\[\033[00m\]:\[\033[01;34m\]\w\[\033[00m\]\$ '
else
    PS1='[${debian_chroot:+($debian_chroot)}\u@\h \W]\$ '
fi
unset color_prompt force_color_prompt

# If this is an xterm set the title to user@host:dir
case "$TERM" in
xterm*|rxvt*)
    PS1="\[\e]0;${debian_chroot:+($debian_chroot)}\u@\h: \w\a\]$PS1"
    ;;
*)
    ;;
esac

# Alias definitions.
# You may want to put all your additions into a separate file like
# ~/.bash_aliases, instead of adding them here directly.
# See /usr/share/doc/bash-doc/examples in the bash-doc package.

if [ -f ~/.bash_aliases ]; then
    . ~/.bash_aliases
fi

# enable programmable completion features (you don't need to enable
# this, if it's already enabled in /etc/bash.bashrc and /etc/profile
# sources /etc/bash.bashrc).
if [ -f /etc/bash_completion ] && ! shopt -oq posix; then
    . /etc/bash_completion
fi
