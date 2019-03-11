#!/usr/bin/env bash

# -----------------------------------------------------------------------------
#
# Pulls latest version of docker images for stuff I use.
#
# -----------------------------------------------------------------------------

set -x

HOST_LDAP_PORT=${HOST_LDAP_PORT:-389}
CONT_LDAP_PORT=${CONT_LDAP_PORT:-10389}
HOST_PSQL_PORT=${HOST_PSQL_PORT:-5432}
CONT_PSQL_PORT=${CONT_PSQL_PORT:-5432}
HOST_MNGO_PORT=${HOST_MNGO_PORT:-27017}
CONT_MNGO_PORT=${CONT_MNGO_PORT:-27017}
HOST_MQTT_PORT=${HOST_MQTT_PORT:-1883}
CONT_MQTT_PORT=${CONT_MQTT_PORT:-1883}
HOST_REDS_PORT=${HOST_REDS_PORT:-6379}
CONT_REDS_PORT=${CONT_REDS_PORT:-6379}

create_ldap() {

  CONTAINER_NAME=${FUNCNAME##create_}
  docker container inspect $CONTAINER_NAME >/dev/null 2>&1
  CONTAINER_EXISTS=$?

  if [[ $CONTAINER_EXISTS -ne 0 ]]; then

    # Make the local directories.
    mkdir -p $HOME/etc/apacheds 2>/dev/null
    mkdir -p $HOME/var/apacheds/{cache,run,log,partitions} 2>/dev/null

    # Create the container.
    docker create \
      -p $HOST_LDAP_PORT:$CONT_LDAP_PORT \
      -v $HOME/etc/apacheds/config.ldif:/bootstrap/conf/config.ldif \
      -v $HOME/var/apacheds/cache:/bootstrap/cache \
      -v $HOME/var/apacheds/run:/boostrap/run \
      -v $HOME/var/apacheds/log:/boostrap/log \
      -v $HOME/var/apacheds/partitions:/bootstrap/partitions \
      --name $CONTAINER_NAME \
      openmicroscopy/apacheds
  fi
}

create_postgres() {

  CONTAINER_NAME=${FUNCNAME##create_}
  docker container inspect $CONTAINER_NAME >/dev/null 2>&1
  CONTAINER_EXISTS=$?

  if [[ $CONTAINER_EXISTS -ne 0 ]]; then

    # Make the local directories.
    mkdir -p $HOME/var/postgres/data 2>/dev/null
    mkdir -p $HOME/etc/postgres 2>/dev/null

    # Copy the config file to the local directory
    docker run -i --rm postgres \
      cat /usr/share/postgresql/postgresql.conf.sample >$HOME/etc/postgres/postgres.conf

    # Create the container.
    docker create \
      -p $HOST_PSQL_PORT:$CONT_PSQL_PORT \
      -v $HOME/etc/postgres/postgres.conf:/etc/postgresql/postgresql.conf \
      -v $HOME/var/postgres/data:/var/lib/postgresql/data \
      --name $CONTAINER_NAME \
      -e POSTGRES_PASSWORD=abcd1234 \
      postgres
  fi
}

create_mongo() {

  CONTAINER_NAME=${FUNCNAME##create_}
  docker container inspect $CONTAINER_NAME >/dev/null 2>&1
  CONTAINER_EXISTS=$?

  if [[ $CONTAINER_EXISTS -ne 0 ]]; then

    # Make the local directories.
    mkdir -p $HOME/var/mongo/data/db 2>/dev/null

    # Create the container.
    docker create \
      -p $HOST_MNGO_PORT:$CONT_MNGO_PORT \
      -v $HOME/var/mongo/data/db:/data/db \
      --name $CONTAINER_NAME \
      -e MONGO_INITDB_ROOT_USERNAME=mongoadmin \
      -e MONGO_INITDB_ROOT_PASSWORD=abcd1234 \
      mongo
  fi
}

create_mqtt() {

  CONTAINER_NAME=${FUNCNAME##create_}
  docker container inspect $CONTAINER_NAME >/dev/null 2>&1
  CONTAINER_EXISTS=$?

  if [[ $CONTAINER_EXISTS -ne 0 ]]; then

    # Make the local directories.
    mkdir -p $HOME/etc/eclipse-mosquitto 2>/dev/null
    mkdir -p $HOME/var/eclipse-mosquitto/{data,log} 2>/dev/null

    # Write the configuration file.
    cat <<-EOF >$HOME/etc/eclipse-mosquitto/mosquitto.conf
		persistence true
		persistence_location /mosquitto/data/
		log_dest file /mosquitto/log/mosquitto.log
EOF

    # Create the container.
    docker create \
      -p $HOST_MQTT_PORT:$CONT_MQTT_PORT \
      -v /mosquitto/config:$HOME/etc/eclipse-mosquitto \
      -v /mosquitto/data:$HOME/var/eclipse-mosquitto/data \
      -v /mosquitto/log:$HOME/var/eclipse-mosquitto/log \
      --name $CONTAINER_NAME \
      eclipse-mosquitto
  fi
}

create_redis() {

  CONTAINER_NAME=${FUNCNAME##create_}
  docker container inspect $CONTAINER_NAME >/dev/null 2>&1
  CONTAINER_EXISTS=$?

  if [[ $CONTAINER_EXISTS -ne 0 ]]; then

    # Create the container.
    docker create \
      -p $HOST_REDS_PORT:$CONT_REDS_PORT \
      --name $CONTAINER_NAME \
      redis
  fi
}

if [[ $(command -v docker) ]]; then

  for IMG in openmicroscopy/apacheds postgres mongo eclipse-mosquitto redis; do
    docker inspect --type=image ${IMG}:latest >/dev/null 2>&1
    IMG_NOT_INSTALLED=$?
    if [[ $IMG_NOT_INSTALLED -ne 0 ]]; then
      docker pull ${IMG}:latest
    fi
  done

  mkdir -p $HOME/{etc,var,tmp} 2>/dev/null

  create_ldap
  create_postgres
  create_mongo
  create_mqtt
  create_redis
fi
