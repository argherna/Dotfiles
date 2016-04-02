#!/bin/bash

if [ $# -eq 0 ]; then
    echo "Usage: ${0##*/} VERS [VERS VERS ...]"
    exit 2
fi

if [ $(id -u) -ne 0 ]; then
    echo "Permission denied!"
	exit 2
fi

exec 1> >(logger -t ${0##*/}) 2>&1

for vers in "$@"; do
    printf "Purging linux-image-extra-3.13.0-%s-generic\n" "$vers"
    dpkg --purge linux-image-extra-3.13.0-${vers}-generic
    printf "Purging linux-image-3.13.0-%s-generic\n" "$vers"
    dpkg --purge linux-image-3.13.0-${vers}-generic
    printf "Purging linux-headers-3.13.0-%s-generic\n" "$vers"
    dpkg --purge linux-headers-3.13.0-${vers}-generic
    printf "Purging linux-headers-3.13.0-%s\n" "$vers"
    dpkg --purge linux-headers-3.13.0-${vers}
done
