# ------------------------------------------------------------------------------
#
# ~/.bash_profile
#
# executed by the command interpreter for login shells. The presence of this 
# file means that the ~/.profile file will be ignored.
#
# ------------------------------------------------------------------------------

# include ~/.bashrc if it exists
if [ -f "$HOME/.bashrc" ]; then
    . "$HOME/.bashrc"
fi



##
# Your previous /Users/agherna/.bash_profile file was backed up as /Users/agherna/.bash_profile.macports-saved_2014-03-28_at_08:31:57
##

# MacPorts Installer addition on 2014-03-28_at_08:31:57: adding an appropriate PATH variable for use with MacPorts.
export PATH=/opt/local/bin:/opt/local/sbin:$PATH
# Finished adapting your PATH environment variable for use with MacPorts.

