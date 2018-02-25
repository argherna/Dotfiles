# -----------------------------------------------------------------------------
# 
# docker-functions.sh
#
# Collection of functions and aliases to use when running Docker operations.
#
# Functions and aliases all begin with 'd'. If the function or alias will
# use docker-compose, then it will begin with 'dc'.
#
# -----------------------------------------------------------------------------


# -----------------------------------------------------------------------------
#
#                         Docker Functions and Aliases
#
# -----------------------------------------------------------------------------

dbth() {
  if [[ $# -lt 3 ]]; then
    cat <<-ENDOFHELP
	Build a container in the given directory, tagging it with the given name,
	and adding hosts as given.

	Usage: $FUNCNAME <dirname> <tagname> <host-ip> [<host-ip> ...]

	  <dirname>    The directory to build the container from.
	  <tagname>    The name to use to tag the new container.
	  <host-ip>    Host and IP address combination to add to the
	               container in host-name:ip-addr format.
ENDOFHELP
    return 1
  fi

  local BUILD_DIR="$1"
  shift
  local TAG_NAME="$1"
  shift
  local ADD_HOSTS=
  if [[ $# -ge 1 ]]; then
    ADD_HOSTS="--add-host=$1"
    shift
    HOST_IPS=( "$@" )
    for HOST_IP in ${HOST_IPS[@]}; do
      ADD_HOSTS="$ADD_HOSTS --add-host=${HOST_IP}"
    done
  fi

  docker build $BUILD_DIR \
      --tag $TAG_NAME \
      $ADD_HOSTS
}

dbt() {
  if [ $# -lt 1 ]; then
    cat <<-ENDOFHELP
	Build a container in the given directory tagging it with the given tagnames.

	Usage $FUNCNAME <dirname> [<tagname> ...]

	  <dirname>    The directory to build the container from.
	  <tagname>    The name(s) to use to tag the new container.
ENDOFHELP
    return 1
  fi

  ARGS="$1"
  shift
  if [ $# -ge 2 ]; then
    ARGS="$ARGS -t $@"
  fi

  docker build $ARGS
}


dexsh() {
  if [ $# -ne 1 ]; then
    cat <<-ENDOFHELP
	Run a Bourne shell (sh) in the specified Docker container.

	Usage: $FUNCNAME <container-id>

	  <container-id>   The Docker container id
ENDOFHELP
    return 1
  fi
  
  docker exec -it $1 /bin/sh
}

dexbash() {
  if [ $# -ne 1 ]; then
    cat <<-ENDOFHELP
	Run a Bash shell (bash) in the specified Docker container.

	Usage: $FUNCNAME <container-id>

	  <container-id>   The Docker container id
ENDOFHELP
    return 1
  fi
  
  docker exec -it $1 /bin/bash
}

dinsfp() {
  if [ $# -ne 2 ]; then
    cat <<-ENDOFHELP
	Prints the information specified in the Go-template from the 'inspect' 
	command for the given Docker container.

	Usage: $FUNCNAME <Go-template> <container-id>

	  <Go-template>    The Go template containing what should be printed
	  <container-id>   The Docker container id

	See <https://golang.org/pkg/text/template/> and 
	<https://docs.docker.com/engine/reference/commandline/inspect/#examples>
	for information about Go-templates and how docker inspect uses them.
ENDOFHELP
    return 1
  fi

  printf "$(docker inspect --format="$1" $2)"
}

dhost() {
  if [ $# -ne 1 ]; then
    cat <<-ENDOFHELP
	Print the Docker Container's host name.

	Usage: $FUNCNAME <container-id>

	  <container-id>   The Docker container id
ENDOFHELP
    #echo "Usage: $FUNCNAME <container-id>"
    return 1
  fi

  printf "$(dinsfp '{{.Config.Hostname}}' $1)"
}

dip() {
  if [ $# -ne 1 ]; then
    cat <<-ENDOFHELP
	Print the IP Address of the given Docker container.

	Usage: $FUNCNAME <container-id>

	  <container-id>   The Docker container id
ENDOFHELP
    return 1
  fi

  printf "$(dinsfp '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' $1)"
}

dhostip() {
  if [ $# -ne 1 ]; then
    cat <<-ENDOFHELP
	Prints the HOSTNAME:IP-ADDRESS of the given Docker container.

	Usage: $FUNCNAME <container-id>

	  <container-id>   The Docker container id
ENDOFHELP
    return 1
  fi

  printf "$(dhost $1):$(dip $1)"
}

alias datt='docker attach'
# Credit to <https://gist.github.com/bastman/5b57ddb3c11942094f8d0a97d461b430#remove-docker-images>
alias dclimg='docker rmi $(docker images --filter "dangling=true" -q --no-trunc)'
alias ddiff='docker diff'
alias deb='dexbash'
alias des='dexsh'
alias dimg='docker images'
alias dins='docker inspect'
alias dlogs='docker logs'
alias dps='docker ps'
# Credit to <https://stackoverflow.com/a/21928864/37776>
alias drestartf='docker start $(docker ps -ql) && docker attach $(docker ps -ql)'
alias drm='docker rm'
alias drmi='docker rmi'
alias drun='docker run'
alias dstart='docker start'
alias dstop='docker stop'
