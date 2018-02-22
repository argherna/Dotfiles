#!/bin/bash

# -----------------------------------------------------------------------------
#
# NAME
#       manage-apacheds-docker.sh
#
# SYNOPSIS
#       manage-apacheds-docker.sh [init|load|rm|start|stop|stoprm]
#
# DESCRIPTION
#       Control a Docker container of an ApacheDS. The purpose of this 
# 		script is to ensure that ApacheDS is shut down correctly in the 
#       container to avoid corrupting partitions. Use these commands:
#       init
#       	Creates and runs the Docker container and loads it 
#           with data in file specified by the APACHEDS_DATA 
#           environment variable.
#
#		load
#			Loads the ApacheDS instance with the data in file
#			specified by the APACHEDS_DATA environment variable.
#
#		rm
#			Deletes the Docker container if it's not running.
#
#		start
#			Starts the Docker container if not running.
#
#		stop
#			Stops the Docker container.
#
#		stoprm
#			Stops and deletes the Docker container.
#
#
# ENVIRONMENT
#       APACHEDS_ADMIN_USERNAME
#			Name of the admin user for ApacheDS. This is used
#           for the init option to connect and load data in the
#           APACHEDS_DATA file.
#
#       APACHEDS_ADMIN_PASSWORD
#			Password of the admin user for ApacheDS. This is 
#           used for the init option to connect and load data 
#			in the APACHEDS_DATA file.
#
#		APACHEDS_DATA
#			Ldif file with bootstrap directory data. Data in 
#			this file is loaded by the init command.
#
#		APACHEDS_INSTANCE
#			Name of the ApacheDS instance to manage (default is 
#			default).
#
#		DOCKER_CONTAINER
#			Name to give to the Docker container (default is ldap).
#
#		DOCKER_IMG
#			Name of the docker image (default is h3nrik/apacheds).
#
#		DOCKER_PORTS
#			Array of ports to publish when creating the container (
#			default is (389:10389 88:60088)).
#
#       VAPACHEDS_CONFIG
#			Points to the apacheds configuration to use.
#
#		VAPACHEDS_DATA
#			Points to the apacheds data directory volume.
#
# -----------------------------------------------------------------------------

APACHEDS_ADMIN_USERNAME="uid=admin,ou=system"
APACHEDS_ADMIN_PASSWORD=secret
APACHEDS_DATA=$HOME/etc/apacheds/nvpapi-sample-directory.ldif
APACHEDS_INSTANCE=default
APACHEDS_STOP_CMD="/opt/apacheds-2.0.0-M24/bin/apacheds stop"
DOCKER_CONTAINER=ldap
DOCKER_IMG=h3nrik/apacheds
DOCKER_PORTS=(389:10389 88:60088 464:60464)
SCRIPT_NAME=$(basename $0)
VAPACHEDS_CONFIG=$HOME/etc/apacheds/config.ldif
VAPACHEDS_DATA=$HOME/var/lib/docker/$APACHEDS_INSTANCE
#VAPACHEDS_HOSTS=$HOME/etc/apacheds/hosts
VAPACHEDS_KRB5=$HOME/etc/apacheds/krb5.conf

usage() {
	cat <<-EOF
	Manage an instance of ApacheDS running in a Docker container.

	Usage: $SCRIPT_NAME [init|load|rm|start|stop|stoprm]

	Commands:

	init    Build and run the ldap container from the image.
	load    Load an ldif into the (running) ldap container.
	rm      Remove the ldap container (must not be running).
	start   Start the ldap container.
	stop    Stop the ldap container.
	stoprm	Stop and remove the ldap container.

	Environment:

	APACHEDS_ADMIN_USERNAME
	    Name of the admin user for ApacheDS. This is used
	    for the init option to connect and load data in the
	    APACHEDS_DATA file.

	APACHEDS_ADMIN_PASSWORD
	    Password of the admin user for ApacheDS. This is 
	    used for the init option to connect and load data 
	    in the APACHEDS_DATA file.
		
	APACHEDS_DATA
	    Ldif file with bootstrap directory data. Data in 
	    this file is loaded by the init command.

	APACHEDS_INSTANCE
	    Name of the ApacheDS instance to manage (default is 
	    default).

	APACHEDS_STOP_CMD
	    Command to run that stops ApacheDS inside the 
	    container.

	DOCKER_CONTAINER
	    Name to give to the Docker container (default is ldap).

	DOCKER_IMG
	    Name of the docker image (default is h3nrik/apacheds).

	DOCKER_PORTS
	    Array of ports to publish when creating the container (
	    default is (389:10389 88:60088)).

	VAPACHEDS_CONFIG
	    Points to the apacheds configuration to use.

	VAPACHEDS_DATA
	    Points to the apacheds data directory volume.

	VAPACHEDS_KRB5
	    Points to the krb5.conf file to use inside the
	    container.
	EOF
}

do_apacheds_docker_stop() {
	docker exec \
		-it $DOCKER_CONTAINER \
		/bin/bash -c "$APACHEDS_STOP_CMD $APACHEDS_INSTANCE"
	docker stop $DOCKER_CONTAINER
}

do_apacheds_docker_rm() {
	docker rm $DOCKER_CONTAINER
}

do_load() {
	ldapmodify -h localhost \
		-D $APACHEDS_ADMIN_USERNAME \
		-w $APACHEDS_ADMIN_PASSWORD \
		-a -f $APACHEDS_DATA
}

if [[ $# -lt 1 ]]; then
	usage
	exit 1
fi

CMD=$1
IS_RUNNING=$(docker ps --filter "name=$DOCKER_CONTAINER" --filter "status=running" -q)
case $CMD in
    init)
	    PORTS=
		if [[ ${#DOCKER_PORTS[@]} -gt 0 ]]; then
		  for PORT in ${DOCKER_PORTS[@]}; do
		    PORTS="$PORTS --publish $PORT"
		  done
		fi
	    docker run \
		  --detach \
		  --name $DOCKER_CONTAINER \
		  $PORTS \
		  --volume $VAPACHEDS_CONFIG:/bootstrap/conf/config.ldif \
		  --volume $VAPACHEDS_DATA:/var/lib/apacheds-2.0.0-M24 \
		  --volume $VAPACHEDS_KRB5:/etc/krb5.conf \
		  $DOCKER_IMG
		;;
	load)
		do_load
		;;
	rm)
		[[ ! -z "$IS_RUNNING" ]] && echo "Can't delete running container." && exit 1
		do_apacheds_docker_rm
		;;
	start)
		[[ -z "$IS_RUNNING" ]] && docker start $DOCKER_CONTAINER
		;;
	stop)
	    [[ -z "$IS_RUNNING" ]] && echo "Container is not running." && exit 1
		do_apacheds_docker_stop
		;;
	stoprm)
	    [[ -z "$IS_RUNNING" ]] && echo "Container is not running." && exit 1
		do_apacheds_docker_stop
		do_apacheds_docker_rm
		;;
	*)
		echo "Unknown command $1"
		usage
		exit 1
		;;
esac
