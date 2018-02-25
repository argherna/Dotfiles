# ------------------------------------------------------------------------------
#
# ~/.bashrc
#
# executed by bash(1) for non-login shells.
#
# ------------------------------------------------------------------------------

# If not running interactively, don't do anything
#
[[ -z "$PS1" ]] && return

# Source in the things I expect EVERYWHERE
#
[[ -f $HOME/.profile.agherna ]] && . $HOME/.profile.agherna

# ------------------------------------------------------------------------------
#
#                            Environment Variables
#
# ------------------------------------------------------------------------------

# ------------------------------------------------------------------------------
#
#                              Set Shell Options
#
# ------------------------------------------------------------------------------
shopt -s checkwinsize
shopt -s histappend

# make less more friendly for non-text input files, see lesspipe(1)
#
[[ -x /usr/bin/lesspipe ]] && eval "$(SHELL=/bin/sh lesspipe)"

# set variable identifying the chroot you work in (used in the prompt below)
#
if [[ -z "$debian_chroot" ]] && [[ -r /etc/debian_chroot ]]; then
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
    xterm-256color) color_prompt=yes;;
esac

# See <https://coderwall.com/p/fasnya/add-git-branch-name-to-bash-prompt>
#
if [[ $color_prompt = yes ]] && [[ -x $(which git 2>/dev/null) ]]
then
  parse_git_branch() {
    git branch 2> /dev/null | sed -e '/^[^*]/d' -e 's/* \(.*\)/ (\1)/'
  }
  export PS1="[\u@\h \[\033[32m\]\W\[\033[00m\]]\[\033[33m\]\$(parse_git_branch)\[\033[00m\] $ "
fi

# Alias definitions.
# You may want to put all your additions into a separate file like
# ~/.bash_aliases, instead of adding them here directly.
# See /usr/share/doc/bash-doc/examples in the bash-doc package.

if [[ -f ~/.bash_aliases ]]; then
    . ~/.bash_aliases
fi

# enable programmable completion features (you don't need to enable
# this, if it's already enabled in /etc/bash.bashrc and /etc/profile
# sources /etc/bash.bashrc).
if [[ -f /etc/bash_completion ]] && ! shopt -oq posix; then
    . /etc/bash_completion
fi
