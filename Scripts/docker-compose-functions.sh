# -----------------------------------------------------------------------------
# 
# docker-compose-functions.sh
#
# Collection of functions and aliases to use when running docker-compose 
# operations.
#
# -----------------------------------------------------------------------------

dcexbash() {
  if [ $# -ne 1 ]; then
    cat <<-ENDOFHELP
	Run a Bash shell (bash) in the specified Docker container.

	Usage: $FUNCNAME <container-id>

 	  <container-id>   The Docker container id
ENDOFHELP
    return 1
  fi
  
  docker-compose exec $1 /bin/bash
}

dcexsh() {
  if [ $# -ne 1 ]; then
    cat <<-ENDOFHELP
	Run a Bourne shell (sh) in the specified Docker container.

	Usage: $FUNCNAME <container-id>

 	  <container-id>   The Docker container id
ENDOFHELP
    return 1
  fi
  
  docker-compose exec $1 /bin/sh
}

alias dcb='docker-compose build'
alias dceb='dcexbash'
alias dclogs='docker-compose logs'
alias dcu='docker-compose up'
