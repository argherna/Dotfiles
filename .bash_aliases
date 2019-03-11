# ------------------------------------------------------------------------------
#
# .bash_aliases
#
# ------------------------------------------------------------------------------


# ------------------------------------------------------------------------------
#
#                       Set aliases based on the kernel.
#
# ------------------------------------------------------------------------------

KERNEL=$(uname)
if [[ "$KERNEL" = "Linux" ]]; then

  # Alias tmux to use Ubuntu config file
  #
  alias tmux='tmux -f ~/.ubu.tmux.conf'

elif [[ "$KERNEL" = "Darwin" ]]; then

  # Alias tmux to use OSX config file
  #
  alias tmux='tmux -f ~/.osx.tmux.conf'

  if [[ -f ~/Scripts/macos-functions.sh ]]; then
    . ~/Scripts/macos-functions.sh
  fi
fi


# ------------------------------------------------------------------------------
#
#                                  Variables
#
# ------------------------------------------------------------------------------

export BOLD=$(tput bold)
export NORMAL=$(tput sgr0)

export BLUE=$(tput setaf 21)
export CYAN=$(tput setaf 78)
export GREEN=$(tput setaf 40)
export ORANGE=$(tput setaf 208)
export RED=$(tput setaf 196)

# ------------------------------------------------------------------------------
#
#                                   Aliases
#
# ------------------------------------------------------------------------------

# start tmux in 256 color mode
#
alias t2='tmux -2'

# Useful shortcuts for hsqldb.
#
alias dbmgr='java -jar $HOME/hsqldb-2.4.0/hsqldb/lib/hsqldb.jar'
alias sqltool='java -jar $HOME/hsqldb-2.4.0/hsqldb/lib/sqltool.jar'
alias sqltoolc='java -cp $HOME/hsqldb-2.4.0/hsqldb/lib/sqltool.jar:$HOME/.m2/repository/log4j/log4j/1.2.17/log4j-1.2.17.jar org.hsqldb.cmdline.SqlTool'

alias ap8='autopep8'
alias changeu='echo "changeme-$(uuidgen)"'

# ------------------------------------------------------------------------------
#
#                                  Functions
#
# ------------------------------------------------------------------------------

# Development function only. Call like this:
#
#   color {1..255}
#
# to see all the colors the terminal can produce.
#
color() {
    for c; do
        printf '\e[48;5;%dm%03d' $c $c
    done
    printf '\e[0m \n'
}

print_msg() {

  LEVEL=$1
  shift
  COLOR=
  case "$LEVEL" in
      DEBUG) COLOR=$BLUE
          ;;
      ERROR) COLOR=$RED
          ;;
      INFO) COLOR=$GREEN
          ;;
      WARN) COLOR=$ORANGE
          ;;
      *) COLOR=$NORMAL
          ;;
  esac

  FORMAT_STRING="$1"
  shift
  printf "$BOLD$COLOR[%-5s]:$NORMAL $FORMAT_STRING" "$LEVEL" "$@"
}

debug() {
  if [[ $# -gt 0 ]]; then
    print_msg "DEBUG" "$@"
  fi
}

error() {
  if [[ $# -gt 0 ]]; then
    print_msg "ERROR" "$@"
  fi
}

inform() {
  if [[ $# -gt 0 ]]; then
    print_msg "INFO" "$@"
  fi
}

warn() {
  if [[ $# -gt 0 ]]; then
    print_msg "WARN" "$@"
  fi
}


s2s_scp() {

  if [ $# -lt 3 ]; then
    cat <<-ENDOFHELP
	Secure copy a file from one remote host to another using a directory
	on this computer as temporary storage.

	Usage: $FUNCNAME <src-server> <dst-server> <filename> [<group-name>]

	  <src-server>    The remote host that has the file you want to 
	                  copy.
	  <dst-server>    The remote host where you want to copy the file 
	                  to.
	  <filename>      The name of the file (full path).
	  <group-name>    OPTIONAL group name to set on <dst-server> for 
	                  the file.

	Use this function when admins lock down the servers at your 
	installation to prevent scp working the way it ought to (for 
	security reasons).
	The file will be copied to exactly the same path on <dst-server> as 
	it is on <src-server>.
	The file is copied to a temporary directory on this machine created
	by the mktemp command. The directory and file are deleted after the
	file is copied to <dst-server>.
ENDOFHELP
     return 1
  fi

  local SRC_SERVER=$1
  local DST_SERVER=$2
  local FILENAME=$3
  local GRPNAME=$4

  TEMP_DIR=$(mktemp -d ${TMPDIR}${FUNCNAME}.XXXXXX)

  scp $SRC_SERVER:$FILENAME $TEMP_DIR/$(basename ${FILENAME})
  scp $TEMP_DIR/$(basename ${FILENAME}) $DST_SERVER:$FILENAME
  rm -rf $TEMP_DIR

  ssh $DST_SERVER chmod 660 $FILENAME

  if [[ ! -z $GRPNAME ]]; then
    ssh $DST_SERVER chgrp $GRPNAME $FILENAME
  fi
}

scp_grw() {
  if [ $# -ne 3 ]; then
     cat <<-ENDOFHELP
	 Secure copy a file to a remote host, then grant group read-
	 write permissions to it.

	 Usage: $FUNCNAME <path-to-file> <server-name> <server-path>

	   <path-to-file>    Fully qualified filename of local file to copy.
	   <server-name>     The remote host.
	   <server-path>     Directory to copy the file to on the remote
	                     host.
ENDOFHELP
     return 1
  fi

  local FNAME=$(basename $1)
  scp $1 $2:$3/$FNAME
  ssh $2 chmod 660 $3/$FNAME
}

ssh_chgrp() {
  if [ $# -ne 3 ]; then
    cat <<-ENDOFHELP
	Run chgrp on a file that exists on a remote host.

	Usage: $FUNCNAME <server-name> <grp-name> <path-to-file-on-server>

	  <server-name>               The remote host.
	  <grp-name>                  Name for the group on the remote 
	                              host.
	  <path-to-file-on-server>    Location of the file on the remote 
	                              host.
ENDOFHELP
    return 1
  fi

  ssh $1 chgrp $2 $3
}

headers() {

  if [[ $# -lt 1 ]]; then
    cat <<-ENDOFHELP
	Request HTTP headers from a remote server.

	Usage: $FUNCNAME <server> [<port>]

	  <server>    The remote server.
	  <port>      OPTIONAL port to communicate on (default is 80).
ENDOFHELP
  fi

  SERVER=$1
  PORT=${2:-80}

  # "Open a read-write file descriptor (5) to PORT on SERVER using tcp"
  #
  exec 5<> /dev/tcp/$SERVER/$PORT
  echo -e "HEAD / HTTP/1.1\nHost: ${SERVER}\n\n" >&5
  cat 0<&5
  (( $? == 0 )) && exec 5>&-
}

test_port() {
  if [ $# -lt 2 ]; then
    cat <<-ENDOFHELP
	Connect to a remote host on a given port using the given protocol.

	Usage: $FUNCNAME <server-or-ip> <port> [<protocol>]

	  <server-or-ip>    The remote host address or name.
	  <port>            Port to communicate on.
	  <protocol>        OPTIONAL protocol to use (default is tcp).
ENDOFHELP
    return 1
  fi

  SERVER=$1
  PORT=$2
  PROTO=${3:-tcp}
  
  # "Open a read-write file descriptor (5) to PORT on SERVER using PROTOCOL"
  #
  exec 5<>/dev/$PROTO/$SERVER/$PORT
  (( $? == 0 )) && exec 5<&-
}

alias tp='test_port'
alias tpl='test_port localhost'

file_size() {
  if [ $# -eq 0 ]; then
    echo "Oh dear"
  fi

  FNAME=$1
  printf "%d\n" $(wc -c <$FNAME)
}

alias fsz='file_size'

# Show all the names (CNs and SANs) listed in the SSL certificate
# for a given domain
function getcertnames() {
	if [ -z "${1}" ]; then
		echo "ERROR: No domain specified.";
		return 1;
	fi;

	local domain="${1}";
	echo "Testing ${domain}…";
	echo ""; # newline

	local tmp=$(echo -e "GET / HTTP/1.0\nEOT" \
		| openssl s_client -connect "${domain}:443" -servername "${domain}" 2>&1);

	if [[ "${tmp}" = *"-----BEGIN CERTIFICATE-----"* ]]; then
		local certText=$(echo "${tmp}" \
			| openssl x509 -text -certopt "no_aux, no_header, no_issuer, no_pubkey, \
			no_serial, no_sigdump, no_signame, no_validity, no_version");
		echo "Common Name:";
		echo ""; # newline
		echo "${certText}" | grep "Subject:" | sed -e "s/^.*CN=//" | sed -e "s/\/emailAddress=.*//";
		echo ""; # newline
		echo "Subject Alternative Name(s):";
		echo ""; # newline
		echo "${certText}" | grep -A 1 "Subject Alternative Name:" \
			| sed -e "2s/DNS://g" -e "s/ //g" | tr "," "\n" | tail -n +2;
		return 0;
	else
		echo "ERROR: Certificate not found.";
		return 1;
	fi;
}

# ------------------------------------------------------------------------------
#                           Terrgrunt and Terraform
# ------------------------------------------------------------------------------

alias tg_apply='terragrunt apply'
alias tg_out='terragrunt output'
alias tg_plan='terragrunt plan'
alias tg_reset='rm -rf ~/.terragrunt'


# ------------------------------------------------------------------------------
#
#                  Source convenience functions by "category"
#
# ------------------------------------------------------------------------------

if [[ ! -z $(which docker) ]] && [[ -f $HOME/Scripts/docker-functions.sh ]]
then
  source $HOME/Scripts/docker-functions.sh
fi

if [[ ! -z $(which docker-compose) ]] && [[ -f $HOME/Scripts/docker-compose-functions.sh ]]
then
  source $HOME/Scripts/docker-compose-functions.sh
fi

if [[ ! -z $(which aws) ]] && [[ -f $HOME/Scripts/aws-functions.sh ]]
then
  source $HOME/Scripts/aws-functions.sh
fi

